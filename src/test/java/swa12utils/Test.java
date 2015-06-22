package swa12utils;

import org.apache.log4j.PropertyConfigurator;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import minder.as4Utils.AS4Utils;
import minder.as4Utils.Corner;
import org.w3c.dom.NodeList;

import static minder.as4Utils.AS4Utils.*;

import javax.xml.soap.SOAPMessage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.List;

/**
 * Created by yerlibilgin on 25/05/15.
 */
public class Test {

  public static final String SAMPLE2_SOAP_MSG = readFile("samples/soap2.xml");

  @BeforeClass
  public static void init() throws WSSecurityException {
    PropertyConfigurator.configure("logging.properties");
    AS4Utils.init();
  }

  @org.junit.Test
  public void main() throws ClassNotFoundException {
    System.out.println(String[].class);
    Class.forName("[Ljava.lang.String;", false, this.getClass().getClassLoader());
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
    SOAPMessage message1 = deserializeSOAPMessage(new FileInputStream("samples/soap_serialized.bin"), true);
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
    SOAPMessage message1 = deserializeSOAPMessage(new FileInputStream("samples/soap_serialized.bin"), true);
    System.out.println(describe(message1));
    SOAPMessage plain1 = deserializeSOAPMessage(serializeSOAPMessage(message1.getMimeHeaders(), message1));
    System.out.println(describe(plain1));
    SOAPMessage plain2 = deserializeSOAPMessage(serializeSOAPMessage(plain1.getMimeHeaders(), plain1));
    System.out.println(describe(plain2));
    SOAPMessage plain3 = deserializeSOAPMessage(serializeSOAPMessage(plain2.getMimeHeaders(), plain2));
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

  @org.junit.Test
  public void testDecryptEncryptDecrypt() throws Exception {
    SOAPMessage message1 = deserializeSOAPMessage(new FileInputStream("samples/soap_serialized.bin"));

    Node firstChild = message1.getSOAPHeader().getFirstChild();
    System.out.println(firstChild.getLocalName());

    firstChild.getAttributes().removeNamedItem("xmlns");


    writeFile("/Users/yerlibilgin/Desktop/original.xml", prettyPrint(message1.getSOAPHeader().getFirstChild().getNextSibling()));

    SOAPMessage plain1 = verifyAndDecrypt(message1, Corner.CORNER_3);
    System.out.println(describe(plain1));

    SOAPMessage packed = encryptAndSign(plain1, Corner.CORNER_3);
    System.out.println(describe(packed));

    writeFile("/Users/yerlibilgin/Desktop/1.xml", prettyPrint(packed.getSOAPHeader().getFirstChild()));

    SOAPMessage plain2 = verifyAndDecrypt(packed, Corner.CORNER_2);
    System.out.println(describe(plain2));

    SOAPMessage packed2 = encryptAndSign(plain2, Corner.CORNER_2);
    System.out.println(describe(packed2));

    writeFile("/Users/yerlibilgin/Desktop/2.xml", prettyPrint(packed2.getSOAPHeader().getFirstChild()));

    SOAPMessage plain3 = verifyAndDecrypt(packed2, Corner.CORNER_3);
    System.out.println(describe(plain3));
  }


  @org.junit.Test
  public void testSignAndVerify() throws Exception {
    SOAPMessage message1 = deserializeSOAPMessage(new FileInputStream("samples/soap_serialized.bin"));

    Node firstChild = message1.getSOAPHeader().getFirstChild();
    System.out.println(firstChild.getLocalName());

    firstChild.getAttributes().removeNamedItem("xmlns");

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

}
