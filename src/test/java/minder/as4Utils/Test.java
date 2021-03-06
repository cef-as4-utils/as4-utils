package minder.as4Utils;

import minder.as4Utils.Corner;
import minder.as4Utils.SWA12Util;
import org.apache.log4j.PropertyConfigurator;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPMessage;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Base64;
import java.util.List;

import static minder.as4Utils.SWA12Util.*;

/**
 * Created by yerlibilgin on 25/05/15.
 */
public class Test {

  public static final String SAMPLE2_SOAP_MSG = readFile("samples/soap2.xml");

  @BeforeClass
  public static void init() throws WSSecurityException {
    PropertyConfigurator.configure("logging.properties");


    try {
      String basedir = "certs2";
      String gateway = "as4-net";

      SWA12Util.init(gateway + "-c2", "123456", gateway + "-c3", "123456", "trust", "123456", readBinaryFile(basedir + "/" + gateway + "-c2.jks"),
         readBinaryFile(basedir + "/" + gateway + "-c3.jks"), readBinaryFile(basedir + "/trust.jks"));
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
    SOAPMessage message1 = deserializeSOAPMessage(new FileInputStream("samples/soap.bin"));
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
    String file = "message1.txt";
    SOAPMessage message1 = deserializeSOAPMessage(new FileInputStream("samples/" + file));

    SOAPMessage signed = signAndEncrypt(message1, Corner.CORNER_3);
    System.out.println(describe(signed));

    writeFile("samples/signed.xml", prettyPrint(signed.getSOAPHeader().getFirstChild()));

    SOAPMessage plain2 = verifyAndDecrypt(signed, Corner.CORNER_2);
    System.out.println(describe(plain2));

    SOAPMessage packed2 = sign(plain2, Corner.CORNER_2);
    System.out.println(describe(packed2));

    writeFile("samples/plain.xml", prettyPrint(packed2.getSOAPHeader().getFirstChild()));

    SOAPMessage plain3 = verifyAndDecrypt(packed2, Corner.CORNER_3);
    System.out.println(describe(plain3));
  }


  @org.junit.Test
  public void testMultipartMime() throws Exception {
    String file = "message1.txt";
    FileInputStream fileInputStream = new FileInputStream("samples/" + file);
    SOAPMessage message2 = deserializeSOAPMessage(fileInputStream);
    System.out.println(describe(message2));
    SOAPMessage soapMessage = signAndEncrypt(message2, Corner.CORNER_2);
    System.out.println(describe(soapMessage));
    soapMessage.writeTo(new FileOutputStream("samples/encrypted2.dat"));

    SOAPMessage soapMessage1 = verifyAndDecrypt(message2, Corner.CORNER_3);
    System.out.println(describe(soapMessage1));
  }


  @org.junit.Test
  public void testPureXMlMessage() throws Exception {
    String file = "message2.txt";
    FileInputStream fileInputStream = new FileInputStream("samples/" + file);
    SOAPMessage message2 = deserializeSOAPMessage(fileInputStream);
    System.out.println(describe(message2));
    SOAPMessage soapMessage = signAndEncrypt(message2, Corner.CORNER_2);
    System.out.println(describe(soapMessage));

    SOAPMessage soapMessage1 = verifyAndDecrypt(message2, Corner.CORNER_3);
    System.out.println(describe(soapMessage1));
  }


  @org.junit.Test
  public void testReadIbmMessage() throws Exception {
    String file = "C2-to-AS4Interceptor.dat";
    FileInputStream fileInputStream = new FileInputStream("samples/" + file);
    SOAPMessage message2 = deserializeSOAPMessage(fileInputStream);
    System.out.println(describe(message2));

    SOAPMessage soapMessage1 = verifyAndDecrypt(message2, Corner.CORNER_3);
    System.out.println(describe(soapMessage1));
  }


  @org.junit.Test
  public void testReadInterceptorMessage() throws Exception {
    String file = "AS4Interceptor-to-C3-1.dat";
    FileInputStream fileInputStream = new FileInputStream("samples/" + file);
    SOAPMessage message2 = deserializeSOAPMessage(fileInputStream);
    System.out.println(describe(message2));

    SOAPMessage soapMessage1 = verifyAndDecrypt(message2, Corner.CORNER_3);
    System.out.println(describe(soapMessage1));
  }


  @org.junit.Test
  public void testReadEncryptedMessage() throws Exception {
    String file = "encrypted.dat";
    FileInputStream fileInputStream = new FileInputStream("samples/" + file);
    SOAPMessage message2 = deserializeSOAPMessage(fileInputStream);
    System.out.println(describe(message2));

    SOAPMessage soapMessage1 = verifyAndDecrypt(message2, Corner.CORNER_3);
    System.out.println(describe(soapMessage1));
  }


  @org.junit.Test
  public void testSendMessage() throws Exception {
    String file = "EncryptedData.dat";
    String targetUrl = "mindertestbed.org";
    int port = 15001;

    System.out.println("Connect");
    Socket socket = new Socket();
    socket.connect(new InetSocketAddress(targetUrl, port));

    System.out.println("Write file");
    FileInputStream fileInputStream = new FileInputStream("samples/" + file);
    byte[] buffer = new byte[10240];

    int read;

    while ((read = fileInputStream.read(buffer)) > 0) {
      System.out.println("Read " + read);
      socket.getOutputStream().write(buffer, 0, read);
      socket.getOutputStream().flush();
    }

    System.out.println("Read response");
    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

    String line;

    while ((line = br.readLine()) != null) {
      System.out.println(line);
    }
    socket.close();

    System.out.println("Done");
  }

  public void testSomeIBMMessage() throws Exception {
    disableSslVerification();

    //final SOAPMessage message = createMessage(null, new FileInputStream("samples/payload(51).dat"));
    //final SOAPMessage message = createMessage(null, new FileInputStream("samples/soap_serialized.xml"));
    //final SOAPMessage message = deserializeSOAPMessage(new FileInputStream("samples/david.txt"));
    SOAPMessage message = createMessage();
    final AttachmentPart part = message.createAttachmentPart();
    final byte[] bytes = "muhammet".getBytes();
    part.setContentId("basidnanberi");
    part.setRawContentBytes(bytes, 0, bytes.length, "application/octet-stream");
    message.addAttachmentPart(part);

    SOAPElement element = message.getSOAPHeader().addChildElement("Messaging", "eb", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/");
    element.addTextNode("User Message");
    element = message.getSOAPBody().addChildElement("question");
    element.addTextNode("What is your name?");

    message.saveChanges();

    System.out.println("Describe plain message");
    System.out.println(describe(message));
    System.out.println("==================================================");
    message = signAndEncrypt((SOAPMessage) message, (Corner) Corner.CORNER_2);
    System.out.println("Describe enc message");
    System.out.println(describe(message));
    System.out.println("==================================================");

    message = verifyAndDecrypt(message, Corner.CORNER_3);
    System.out.println("Describe reopened message");
    System.out.println(describe(message));
    System.out.println("==================================================");
  }

}
