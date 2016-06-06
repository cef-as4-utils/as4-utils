package swa12utils;

import org.apache.log4j.PropertyConfigurator;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import minder.as4Utils.SWA12Util;
import minder.as4Utils.Corner;

import static minder.as4Utils.SWA12Util.*;

import javax.xml.bind.DatatypeConverter;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.SOAPMessage;
import java.io.*;
import java.net.URL;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

/**
 * Created by yerlibilgin on 25/05/15.
 */
public class Test {

  public static final String SAMPLE2_SOAP_MSG = readFile("samples/soap2.xml");

  @BeforeClass
  public static void init() throws WSSecurityException {
    PropertyConfigurator.configure("logging.properties");


    try {
      SWA12Util.init("ibmgw-c2", "123456", "ibmgw-c3", "123456", "trust", "123456", readBinaryFile("certs/ibmgw-c2.jks"),
          readBinaryFile("certs/ibmgw-c3.jks"), readBinaryFile("certs/trust.jks"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @org.junit.Test
  public void base64() {
    System.out.println(new String(Base64.getDecoder().decode("TXVoYW1tZXQgWUlMREla")));
  }


  @org.junit.Test
  public void testGZIPUNZIP() {

//    System.out.println(SAMPLE2_SOAP_MSG.length());
//    System.out.println("=======================");
    byte[] compressed = gzip(SAMPLE2_SOAP_MSG.getBytes());

//    System.out.println(new String(compressed));
//    System.out.println(compressed.length);
//    System.out.println("======================");

    byte[] plain = gunzip(compressed);

//    System.out.println(new String(plain));
//    System.out.println(plain.length);

    Assert.assertEquals(new String(plain), SAMPLE2_SOAP_MSG);
  }

  @org.junit.Test
  public void testXPath() throws Exception {
    SOAPMessage message1 = SWA12Util.deserializeSOAPMessage(new FileInputStream("samples/soap.bin"));
    FileOutputStream fos = new FileOutputStream("samples/soap_serialized.xml");
    message1.writeTo(fos);
    fos.close();
    Node node = message1.getSOAPPart();
    System.out.println(prettyPrint(node));

    Element singleNode = findSingleNode(node, "//:PayloadInfo/:PartInfo/:PartProperties");
    System.out.println(prettyPrint(singleNode));

    singleNode.setAttribute("Naber", "Iyidir");
    System.out.println(prettyPrint(singleNode));
    Node attr = findSingleNode(node, "//:PayloadInfo/:PartInfo/:PartProperties/@Naber");
    System.out.println(attr.getTextContent());
    Assert.assertEquals(attr.getTextContent(), "Iyidir");


    Element el = findSingleNode(node, "/:Envelope/:Header/:Messaging/:UserMessage/:PartyInfo");
    Element to = findSingleNode(el, "/:Envelope/:Header/:Messaging/:UserMessage/:PartyInfo/:To");

    el.appendChild(to.cloneNode(true));
    el.appendChild(to.cloneNode(true));
    el.appendChild(to.cloneNode(true));


    System.out.println(prettyPrint(el));
    List<Element> tos = listNodes(el, "/:Envelope/:Header/:Messaging/:UserMessage/:PartyInfo/:To");

    Assert.assertEquals(tos.size(), 4);

    el = findSingleNode(node, "//:MessageId");
    System.out.println(el.getTextContent());


    Element payloadInfoElement = findSingleNode(node, "//:PayloadInfo");
    //print contents
    System.out.println(prettyPrint(payloadInfoElement));
    //remove all children
//    NodeList children = payloadInfoElement.getChildNodes();
//    int i = children.getLength()-1;
//    while(i >= 0) {
//      payloadInfoElement.removeChild(children.item(i));
//      i = i - 1;
//    }
//
//    //now add a single node like this
//    //<eb:PartInfo href="http://www.w3schools.com/xml/cd_catalog.xml" />
//    Element ell = payloadInfoElement.getOwnerDocument().createElementNS(payloadInfoElement.getNamespaceURI(),
//        "eb:PartInfo");
//    ell.setAttribute("href", "http://www.w3schools.com/xml/cd_catalog.xml");
//    payloadInfoElement.appendChild(ell);
//    System.out.println(prettyPrint(node));

    //find the part info for custom payload:
    Element partInfoElement = findSingleNode(payloadInfoElement, "//:PartInfo[@href='submissionAcceptance@e-codex.eu']");

    System.out.println(prettyPrint(partInfoElement));
    Element mim = findSingleNode(partInfoElement, "//:PartInfo[@href='submissionAcceptance@e-codex.eu']/:PartProperties/:Property");
    System.out.println(prettyPrint(mim));


    Element crpyto = findSingleNode(message1.getSOAPPart(), "//:EncryptedData/:EncryptionMethod");
    System.out.println(prettyPrint(crpyto));
    final String algorithm = crpyto.getAttribute("Algorithm");
    System.out.println(algorithm);
  }

  @org.junit.Test
  public void testSerializeDeserializeSerialize() throws Exception {
    System.out.println("EEE");
    SOAPMessage message1 = deserializeSOAPMessage(new FileInputStream("samples/soap.bin"), true);
    final String[] header = message1.getMimeHeaders().getHeader("content-type");
    message1.getMimeHeaders().removeAllHeaders();
    message1.getMimeHeaders().addHeader("content-type", header[0]);
    System.out.println(describe(message1));
    SOAPMessage plain1 = deserializeSOAPMessage(serializeSOAPMessage(message1.getMimeHeaders(), message1));
    System.out.println(prettyPrint(plain1.getSOAPPart()));
    SOAPMessage plain2 = deserializeSOAPMessage(serializeSOAPMessage(null, plain1));
    System.out.println(describe(plain2));
    SOAPMessage plain3 = deserializeSOAPMessage(serializeSOAPMessage(null, plain2));
    System.out.println(describe(plain3));
  }


  public void writeFile(String file, String val) {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file);

      fos.write(val.getBytes());
      fos.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void writeBinaryFile(String file, byte[] val) {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file);

      fos.write(val);
      fos.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @org.junit.Test
  public void testDecryptEncryptDecrypt() throws Exception {
    SOAPMessage message1 = deserializeSOAPMessage(new FileInputStream("samples/david.txt"));

    //Node firstChild = message1.getSOAPHeader().getFirstChild();
    //System.out.println(firstChild.getLocalName());

    //firstChild.getAttributes().removeNamedItem("xmlns");


    writeFile("/Users/yerlibilgin/Desktop/original.xml", prettyPrint(message1.getSOAPHeader().getFirstChild().getNextSibling()));

    SOAPMessage plain1 = verifyAndDecrypt(message1, Corner.CORNER_3);
    System.out.println(describe(plain1));

    SOAPMessage packed = signAndEncrypt(plain1, Corner.CORNER_3);
    System.out.println(describe(packed));

    writeFile("/Users/yerlibilgin/Desktop/1.xml", prettyPrint(packed.getSOAPHeader().getFirstChild()));

    SOAPMessage plain2 = verifyAndDecrypt(packed, Corner.CORNER_2);
    System.out.println(describe(plain2));

    SOAPMessage packed2 = signAndEncrypt(plain2, Corner.CORNER_2);
    System.out.println(describe(packed2));

    writeFile("/Users/yerlibilgin/Desktop/2.xml", prettyPrint(packed2.getSOAPHeader().getFirstChild()));

    SOAPMessage plain3 = verifyAndDecrypt(packed2, Corner.CORNER_3);
    System.out.println(describe(plain3));
  }


  @org.junit.Test
  public void testSignAndVerify() throws Exception {
    SOAPMessage message1 = deserializeSOAPMessage(new FileInputStream("samples/david.txt"));
    message1.writeTo(new FileOutputStream("samples/david.xml"));


    writeFile("/Users/yerlibilgin/Desktop/original.xml", prettyPrint(message1.getSOAPHeader().getFirstChild().getNextSibling()));

    SOAPMessage plain1 = verifyAndDecrypt(message1, Corner.CORNER_3);
    System.out.println(describe(plain1));

    SOAPMessage signed = sign(plain1, Corner.CORNER_3);
    System.out.println(describe(signed));

    writeFile("/Users/yerlibilgin/Desktop/signed.xml", prettyPrint(signed.getSOAPHeader().getFirstChild()));

    SOAPMessage plain2 = verifyAndDecrypt(signed, Corner.CORNER_2);
    System.out.println(describe(plain2));

    SOAPMessage packed2 = sign(plain2, Corner.CORNER_2);
    System.out.println(describe(packed2));

    writeFile("/Users/yerlibilgin/Desktop/plain.xml", prettyPrint(packed2.getSOAPHeader().getFirstChild()));

    SOAPMessage plain3 = verifyAndDecrypt(packed2, Corner.CORNER_3);
    System.out.println(describe(plain3));
  }


  @org.junit.Test
  public void testIBMC3Message() throws Exception {
    String file = "baseplainmessage.txt";
    SOAPMessage message2 = deserializeSOAPMessage(new FileInputStream("samples/" + file));

    System.out.println(describe(message2));
    SOAPMessage plain1 = verifyAndDecrypt(message2, Corner.CORNER_3);
    System.out.println(describe(plain1));
  }

}
