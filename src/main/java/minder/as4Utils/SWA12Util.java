package minder.as4Utils;

import org.apache.log4j.PropertyConfigurator;
import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.bsp.BSPRule;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.AttachmentUtils;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.net.ssl.*;
import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.lang.reflect.Array;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by yerlibilgin on 13/05/15.
 */

public class SWA12Util extends WSS4JConstants {

  public static final String RSA_SHA256 =
     "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

  public static final String E_SENS_DEFAULT = "E_SENS_DEFAULT";
  public static final String INHERIT = "INHERIT";

  public static String XMLENC_symmetricEncAlgorithm = AES_128_GCM;
  public static String XMLENC_keyEncryptionAlgorithm = KEYTRANSPORT_RSAOAEP_XENC11;
  public static String XMLENC_digestAlgorithm = SHA256;


  public static String XMLSIG_signatureAlgorithm = RSA_SHA256;
  private static String XMLSIG_cannonicalizationAlgo = C14N_EXCL_OMIT_COMMENTS;

  static MessageFactory messageFactory = null;
  static SOAPConnectionFactory soapConnectionFactory = null;

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SWA12Util.class);
  private static HashMap<String, byte[]> keyStoreBytes = new HashMap<>();

  static {
    try {
      messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
      soapConnectionFactory = SOAPConnectionFactory.newInstance();
    } catch (Exception ex) {
      LOG.error("Exception: " + ex.toString());
    }
  }

  public static String prettyPrint(org.w3c.dom.Node node) {
    Transformer transformer = null;
    try {
      transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      StreamResult result = new StreamResult(new StringWriter());
      DOMSource source = new DOMSource(node);
      transformer.transform(source, result);
      String xmlString = result.getWriter().toString();
      return xmlString;
    } catch (Exception e) {
      e.printStackTrace();
      return "";
    }
  }

  /**
   * A utility method to create a SOAP1.2 With Attachments message
   *
   * @return
   */
  public static SOAPMessage createMessage() {
    try {
      return messageFactory.createMessage();
    } catch (SOAPException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Create an object array of two items:
   * create a byte array that includes:
   * LENGTH OF HEADER - 4 bytes
   * HEADER
   * LENGTH OF MESSAGE - 4 bytes
   * MESSAGE
   *
   * @param headers
   * @param message
   * @return
   */
  public static byte[] serializeSOAPMessage(MimeHeaders headers, SOAPMessage message) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream daos = new DataOutputStream(baos);


    try {

      //first write the message
      byte[] bodyArray = writeMessageToByeArray(message);
      //now grab the header do this in the second order because after writeMess... the header changes

//      Iterator iterator = message.getMimeHeaders().getAllHeaders();
//
//      String partIdentifier = null;
//      while (iterator.hasNext()) {
//        MimeHeader header = (MimeHeader) iterator.next();
//        if (header.getName().equalsIgnoreCase("content-type")) {
//          int beginIndex = header.getValue().indexOf("boundary=\"");
//          if (beginIndex < 0) {
//            throw new RuntimeException("invalid content type " + header.getValue());
//          }
//
//          beginIndex = header.getValue().indexOf('\"', beginIndex) + 1;
//          int endIndex = header.getValue().indexOf('\"', beginIndex);
//          partIdentifier = header.getValue().substring(beginIndex, endIndex);
//          break;
//        }
//      }
//
//      if (partIdentifier != null) {
//        byte[] headerBytes = partIdentifier.getBytes();
//        daos.write(headerBytes);
//        daos.writeByte('\n');
//      }
      daos.write(bodyArray);
      daos.close();

    } catch (Exception ex) {
    }


    return baos.toByteArray();
  }

  public static byte[] writeMessageToByeArray(SOAPMessage message) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      message.writeTo(baos);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return baos.toByteArray();
  }

  /**
   * Deserialize a soap object serialized by <code>serializeSOAPMessage</code> method
   *
   * @param serialized
   * @return
   */
  public static SOAPMessage deserializeSOAPMessage(byte[] serialized) {
    return deserializeSOAPMessage(new ByteArrayInputStream(serialized));
  }

  public static SOAPMessage deserializeSOAPMessage(InputStream inputStream) {
    PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream, 100);
    try {
      byte[] identifier2 = new byte[100];
      MimeHeaders headers = null;
      int len = pushbackInputStream.read(identifier2, 0, identifier2.length);
      pushbackInputStream.unread(identifier2, 0, len);
      //check the first two characters and see if they are '-'

      if (identifier2[0] == '-' && identifier2[1] == '-') {
        //get the line and parse the part identifier

        byte[] partIdentifier = new byte[100];

        int index = 0;

        byte current = 0;

        do {
          current = (byte) pushbackInputStream.read();
          partIdentifier[index++] = current;
        } while (current != '\n');

        //now put back
        pushbackInputStream.unread(partIdentifier, 0, index);

        String part = new String(partIdentifier, 2, index - 2).trim();

        System.out.println(part);
        headers = new MimeHeaders();
        headers.addHeader("Content-Type", "multipart/related; boundary=\"" + part + "\"; type=\"application/soap+xml\"");
        return messageFactory.createMessage(headers, pushbackInputStream);
      } else {
        //try to read directly
        return messageFactory.createMessage(null, pushbackInputStream);
      }
    } catch (RuntimeException e) {
      LOG.error(e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  public static SOAPMessage readFromByteArray(MimeHeaders headers, byte[] array) throws IOException, SOAPException {
    return messageFactory.createMessage(headers, new ByteArrayInputStream(array));
  }

  /**
   * This method sends a SOAP1.2 message to the given url.
   *
   * @param message
   * @param endpoint
   * @return
   */
  public static SOAPMessage sendSOAPMessage(SOAPMessage message, URL endpoint) {
    try {
      SOAPConnection connection = soapConnectionFactory.createConnection();
      return connection.call(message, endpoint);
    } catch (SOAPException e) {
      throw new RuntimeException(e);
    }
  }

  public static SOAPMessage createMessage(MimeHeaders headers, InputStream is) throws IOException, SOAPException {
    return messageFactory.createMessage(headers, is);
  }

  //region DE/COMPRESSION UTILITIES

  /**
   * compress the given byte array
   *
   * @param plain
   * @return
   */
  public static byte[] gzip(byte[] plain) {
    ByteArrayInputStream bais = new ByteArrayInputStream(plain);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    gzip(bais, baos);
    return baos.toByteArray();
  }


  public static byte[] gunzip(byte[] compressed) {
    ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    gunzip(bais, baos);
    return baos.toByteArray();
  }

  /**
   * Compress the given stream as GZIP
   *
   * @param inputStream
   * @param outputStream
   */
  public static void gzip(InputStream inputStream, OutputStream outputStream) {
    try {
      GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream, true);
      transferData(inputStream, gzipOutputStream);
      gzipOutputStream.close();
    } catch (Exception e) {
      throw new RuntimeException("GZIP Compression failed");
    }
  }

  /**
   * Decompress the given stream that contains gzip data
   *
   * @param inputStream
   * @param outputStream
   */
  public static void gunzip(InputStream inputStream, OutputStream outputStream) {
    try {
      GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
      transferData(gzipInputStream, outputStream);
      gzipInputStream.close();
    } catch (Exception e) {
      throw new RuntimeException("GZIP decompression failed");
    }
  }

  public static void transferData(InputStream gzipInputStream, OutputStream outputStream) throws Exception {
    byte[] chunk = new byte[1024];
    int read = -1;
    while ((read = gzipInputStream.read(chunk, 0, chunk.length)) > 0) {
      outputStream.write(chunk, 0, read);
    }
  }
  //endregion


  //region CRYPTO UTILITIES
  private static WSSecurityEngine secEngine = new WSSecurityEngine();
  private static Crypto c2Crypto = null;
  private static Crypto c3Crypto = null;
  private static Crypto trustCrypto = null;

  private static String USER_C2 = "testGateway1";
  private static String USER_C3 = "testGateway2";
  private static String PWD_C2 = "123456";
  private static String PWD_C3 = "123456";

  public static void init() {
    init("c2.properties", "c3.properties", "trust.properties");
  }

  public static void init(String c2, String c3, String trust) {
    Security.addProvider(new BouncyCastleProvider());
    try {
      setKeyStoreBytes("c2.jks", readResource("c2.jks"));
      setKeyStoreBytes("c3.jks", readResource("c3.jks"));
      setKeyStoreBytes("trst.jks", readResource("trst.jks"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      PropertyConfigurator.configure(
         Thread.currentThread().getContextClassLoader().getResource(
            "logging.properties"));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    try {
      WSSConfig.init();
      c2Crypto = CryptoFactory.getInstance(c2);
      c3Crypto = CryptoFactory.getInstance(c3);
      trustCrypto = CryptoFactory.getInstance(trust);
    } catch (Throwable th) {
      throw new RuntimeException(th);
    }
  }

  public static void init(String c2alias, String c2Password, String c3alias, String c3Password,
                          String trustAlias, String trustPassword,
                          byte[] c2jks, byte[] c3jks, byte[] trustJks) {
    Properties c2Properties = createProperty(c2alias, c2Password);
    Properties c3Properties = createProperty(c3alias, c3Password);
    Properties trustProperties = createProperty(trustAlias, trustPassword);

    try {
      PropertyConfigurator.configure(
         Thread.currentThread().getContextClassLoader().getResource(
            "logging.properties"));
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    setKeyStoreBytes(c2alias, c2jks);
    setKeyStoreBytes(c3alias, c3jks);
    setKeyStoreBytes(trustAlias, trustJks);

    USER_C2 = c2alias;
    USER_C3 = c3alias;
    PWD_C2 = c2Password;
    PWD_C3 = c3Password;

    init(c2Properties, c3Properties, trustProperties);
  }

  public static void init(Properties c2, Properties c3, Properties trust) {
    try {
      PropertyConfigurator.configure(
         Thread.currentThread().getContextClassLoader().getResource(
            "logging.properties"));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    try {
      WSSConfig.init();
      c2Crypto = CryptoFactory.getInstance(c2);
      c3Crypto = CryptoFactory.getInstance(c3);
      trustCrypto = CryptoFactory.getInstance(trust);
    } catch (Throwable th) {
      throw new RuntimeException(th);
    }
  }

  public static byte[] readResource(String s) throws IOException {
    InputStream is = SWA12Util.class.getResourceAsStream(s);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] bytes = new byte[1024];

    int read = -1;

    while ((read = is.read(bytes)) != -1) {
      baos.write(bytes, 0, read);
    }

    return baos.toByteArray();
  }

  public static void setKeyStoreBytes(String name, byte[] bytes) {
    keyStoreBytes.put(name, bytes);
  }

  static InputStream getResource(String name) {
    final byte[] buf = keyStoreBytes.get(name);
    System.out.println(name);
    return new ByteArrayInputStream(buf);
  }

  //region Extract Attachments
  public static SOAPMessage verifyAndDecrypt(SOAPMessage message, Corner receiver) throws Exception {
    SOAPMessage newMessage = deserializeSOAPMessage(serializeSOAPMessage(message.getMimeHeaders(), message));


    final List<Attachment> attachments = parts2att(message);

    AttachmentCallbackHandler attachmentCallbackHandler = new AttachmentCallbackHandler(attachments);
    verify(newMessage.getSOAPPart(), attachmentCallbackHandler, receiver);

    newMessage.removeAllAttachments();
    final List<Attachment> responseAttachments = attachmentCallbackHandler.getResponseAttachments();

    consumeResponseAttachments(newMessage, responseAttachments);

    WSSecHeader header = new WSSecHeader(newMessage.getSOAPPart());
    header.removeSecurityHeader();
    newMessage.saveChanges();

    return newMessage;
  }

  /**
   * Verifies the soap envelope.
   * This method verifies all the signature generated.
   *
   * @throws java.lang.Exception Thrown when there is a problem in verification
   */
  private static WSHandlerResult verify(Document doc, CallbackHandler attachmentCallbackHandler, Corner receiver) throws Exception {
    RequestData requestData = new RequestData();
    List<BSPRule> bspRules = new ArrayList<>();
    bspRules.add(BSPRule.R5621);
    bspRules.add(BSPRule.R5620);
    requestData.setIgnoredBSPRules(bspRules);
    requestData.setAttachmentCallbackHandler(attachmentCallbackHandler);
    requestData.setSigVerCrypto(trustCrypto);
    requestData.setDecCrypto(receiver == Corner.CORNER_2 ? c2Crypto : c3Crypto);
    requestData.setCallbackHandler(new KeystoreCallbackHandler());
    return secEngine.processSecurityHeader(doc, requestData);
  }

  /**
   * Signs the soap message (no encrpytion)
   * <p>
   * Strips out the ws:security header if any.
   * <p>
   * Assumes that the attachments are already decrypted.
   *
   * @param message plain message to be signed
   * @param sender  the corner that sent the message
   */
  public static SOAPMessage sign(SOAPMessage message, Corner sender) {
    return sign(message, sender, null);
  }

  /**
   * Signs the soap message (no encrpytion)
   * <p>
   * Strips out the ws:security header if any.
   * <p>
   * Assumes that the attachments are already decrypted.
   *
   * @param message        plain message to be signed
   * @param sender         the corner that sent the message
   * @param previousHeader The header that the xml security properties will be copied from (maybe null)
   */
  public static SOAPMessage sign(SOAPMessage message, Corner sender, SOAPHeader previousHeader) {
    try {
      message.saveChanges();
      SOAPMessage newMessage = deserializeSOAPMessage(serializeSOAPMessage(message.getMimeHeaders(), message));
      newMessage.removeAllAttachments();
      stripWSSEC(newMessage.getSOAPPart());
      Document doc = newMessage.getSOAPPart();

      WSSecHeader secHeader = new WSSecHeader(doc);
      secHeader.insertSecurityHeader();

      final List<Attachment> attachments = parts2att(message);

      WSSecSignature signature = new WSSecSignature();
      //if C2 is sending, then sign with C2 key.
      signature.setUserInfo(sender == Corner.CORNER_2 ? USER_C2 : USER_C3, sender == Corner.CORNER_2 ? PWD_C2 : PWD_C3);
      signature.getParts().add(new WSEncryptionPart("Messaging", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/", "Content"));
      signature.getParts().add(new WSEncryptionPart("Body", "http://www.w3.org/2003/05/soap-envelope", "Element"));
      signature.getParts().add(new WSEncryptionPart("cid:Attachments", "Content"));


      prepareAlgorithmsForSigning(previousHeader, signature);

      signature.setKeyIdentifierType(WSConstants.BST_DIRECT_REFERENCE);

      AttachmentCallbackHandler attachmentCallbackHandler = new AttachmentCallbackHandler(attachments);
      signature.setAttachmentCallbackHandler(attachmentCallbackHandler);

      //if C2 is sending, then sign with C2 key.
      doc = signature.build(doc, sender == Corner.CORNER_2 ? c2Crypto : c3Crypto, secHeader);
      if (LOG.isDebugEnabled()) {
        String outputString = XMLUtils.PrettyDocumentToString(doc);
        LOG.debug(outputString);
      }

      consumeResponseAttachments(newMessage, attachmentCallbackHandler.getResponseAttachments());

      newMessage.saveChanges();
      return newMessage;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Signs then encrypts soap message together with the attachments.
   * <p>
   * Strips out the ws:security header if any.
   * <p>
   * Assumes that the attachments are already decrypted.
   *
   * @param message The plain message to be signed and encrypted
   * @param sender  Corner that sends the message (C2 or C3)
   */
  public static SOAPMessage signAndEncrypt(SOAPMessage message, Corner sender) {
    return signAndEncrypt(message, sender, null);
  }

  /**
   * Signs then encrypts soap message together with the attachments.
   * <p>
   * Strips out the ws:security header if any.
   * <p>
   * Assumes that the attachments are already decrypted.
   *
   * @param message        The plain message to be signed and encrypted
   * @param sender         Corner that sends the message (C2 or C3)
   * @param previousHeader The header that the xml security properties will be copied from (maybe null)
   */
  public static SOAPMessage signAndEncrypt(SOAPMessage message, Corner sender, SOAPHeader previousHeader) {
    try {
      byte[] serialized = serializeSOAPMessage(message.getMimeHeaders(), message);
      SOAPMessage newMessage = deserializeSOAPMessage(serialized);
      Document doc = newMessage.getSOAPPart();

      WSSecHeader secHeader = new WSSecHeader(doc);
      secHeader.removeSecurityHeader();
      secHeader.insertSecurityHeader();

      WSSecSignature signature = new WSSecSignature();
      //if C2 is sending, then sign with C2 key.
      signature.setUserInfo(sender == Corner.CORNER_2 ? USER_C2 : USER_C3, sender == Corner.CORNER_2 ? PWD_C2 : PWD_C3);
      signature.getParts().add(new WSEncryptionPart("Messaging", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/", "Content"));
      signature.getParts().add(new WSEncryptionPart("Body", "http://www.w3.org/2003/05/soap-envelope", "Element"));

      prepareAlgorithmsForSigning(previousHeader, signature);

      AttachmentCallbackHandler attachmentCallbackHandler = null;
      final int attachmentCount = newMessage.countAttachments();
      if (attachmentCount > 0) {
        signature.getParts().add(new WSEncryptionPart("cid:Attachments", "Content"));
        final List<Attachment> attachments = parts2att(newMessage);
        attachmentCallbackHandler = new AttachmentCallbackHandler(attachments);
        signature.setAttachmentCallbackHandler(attachmentCallbackHandler);
      }

      //if C2 is sending, then sign with C2 key.
      signature.build(doc, sender == Corner.CORNER_2 ? c2Crypto : c3Crypto, secHeader);
      if (LOG.isDebugEnabled()) {
        String outputString = XMLUtils.PrettyDocumentToString(doc);
        LOG.debug(outputString);
      }

      WSSecEncrypt encrypt = new WSSecEncrypt();
      //if C2 is sending, then enrypt with C3 certificate.
      encrypt.setUserInfo(sender == Corner.CORNER_2 ? USER_C3 : USER_C2, sender == Corner.CORNER_2 ? PWD_C3 : PWD_C2);


      prepareAlgorithmsForEncryption(encrypt, previousHeader);

      if (attachmentCount > 0) {
        encrypt.getParts().add(new WSEncryptionPart("cid:Attachments", "Element"));
        List<Attachment> signedAttachments = attachmentCallbackHandler.getResponseAttachments();
        attachmentCallbackHandler = new AttachmentCallbackHandler(signedAttachments);
        encrypt.setAttachmentCallbackHandler(attachmentCallbackHandler);
      }

      //if C2 is sending, then enrypt with C3 certificate.
      encrypt.build(doc, sender == Corner.CORNER_2 ? c3Crypto : c2Crypto, secHeader);

      newMessage.removeAllAttachments();
      if (attachmentCount > 0) {
        consumeResponseAttachments(newMessage, attachmentCallbackHandler.getResponseAttachments());
      }
      newMessage.saveChanges();

      return newMessage;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private static List<Attachment> parts2att(SOAPMessage message) {
    final List<Attachment> attachments = new ArrayList<>();
    message.getAttachments().forEachRemaining(new Consumer<AttachmentPart>() {
      @Override
      public void accept(final AttachmentPart o) {
        final Attachment at = new Attachment();
        o.getAllMimeHeaders().forEachRemaining(hdr -> {
             MimeHeader header = (MimeHeader) hdr;
             at.addHeader(header.getName(), header.getValue());
           }
        );

        at.setMimeType(o.getContentType());
        at.setId(o.getContentId().replaceAll(">|<", ""));
        try {
          at.setSourceStream(new ByteArrayInputStream(o.getRawContentBytes()));
        } catch (SOAPException e) {
          e.printStackTrace();
        }
        attachments.add(at);
      }
    });
    return attachments;
  }

  /**
   * Removes the ws:security element from the given node.
   *
   * @return
   */
  public static void stripWSSEC(Document document) {
    WSSecHeader secHeader = new WSSecHeader(document);
    try {
      secHeader.removeSecurityHeader();
    } catch (WSSecurityException e) {
      e.printStackTrace();
    }
  }

  //endregion


  private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

  static {
    factory.setNamespaceAware(true);
  }


  protected static Map<String, String> getHeaders(String attachmentId) {
    Map<String, String> headers = new HashMap<>();
    headers.put(AttachmentUtils.MIME_HEADER_CONTENT_DESCRIPTION, "Attachment");
    headers.put(AttachmentUtils.MIME_HEADER_CONTENT_DISPOSITION, "attachment; filename=\"fname.ext\"");
    headers.put(AttachmentUtils.MIME_HEADER_CONTENT_ID, "<attachment=" + attachmentId + ">");
    headers.put(AttachmentUtils.MIME_HEADER_CONTENT_LOCATION, "http://ws.apache.org");
    headers.put(AttachmentUtils.MIME_HEADER_CONTENT_TYPE, "text/xml; charset=UTF-8");
    headers.put("TestHeader", "testHeaderValue");
    return headers;
  }

  public static List<AttachmentPart> getAttachments(SOAPMessage msg) {
    final List<AttachmentPart> atp = new ArrayList<>();

    msg.getAttachments().forEachRemaining(new Consumer<AttachmentPart>() {
      @Override
      public void accept(AttachmentPart o) {
        atp.add(o);
      }
    });

    try {
      msg.saveChanges();
    } catch (SOAPException e) {
      throw new RuntimeException(e);
    }
    return atp;
  }


  public static byte[] getAttachmentContent(AttachmentPart atp) {
    try {
      return atp.getRawContentBytes();
    } catch (SOAPException e) {
      throw new RuntimeException(e);
    }
  }

  public static void setAttachmentContent(AttachmentPart atp, byte[] content) {
    try {
      atp.setRawContentBytes(content, 0, content.length, atp.getContentType());
    } catch (SOAPException e) {
      throw new RuntimeException(e);
    }
  }

  public static void replaceAttachments(SOAPMessage msg, List<AttachmentPart> attachmentParts) {
    try {
      msg.removeAllAttachments();
      msg.saveChanges();
      attachmentParts.forEach(msg::addAttachmentPart);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  static XPath xPath = createAS4AwareXpath();

  public static <T> T findSingleNode(org.w3c.dom.Node node, String xpath) {
    try {
      Object o = xPath.evaluate(xpath, node, XPathConstants.NODE);
      if (o == null)
        throw new RuntimeException("No match for [" + xpath + "]");

      return (T) o;
    } catch (XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> List<T> listNodes(org.w3c.dom.Node node, String xpath) {
    try {
      Object o = xPath.evaluate(xpath, node, XPathConstants.NODESET);
      if (o == null)
        throw new RuntimeException("No match for [" + xpath + "]");

      NodeList list = (NodeList) o;

      List<T> els = new ArrayList<>();
      for (int i = 0; i < list.getLength(); ++i) {
        els.add((T) ((NodeList) o).item(i));
      }
      return els;
    } catch (XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }

  public static XPath createAS4AwareXpath() {
    XPath xPath = XPathFactory.newInstance().newXPath();
    xPath.setNamespaceContext(new SoapNamespaceContext());
    return xPath;
  }

  static void disableSslVerification() {
    try {
      // Create a trust manager that does not validate certificate chains
      TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
      }
      };

      // Install the all-trusting trust manager
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

      // Create all-trusting host name verifier
      HostnameVerifier allHostsValid = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      };

      // Install the all-trusting host verifier
      HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (KeyManagementException e) {
      e.printStackTrace();
    }
  }

  static class SoapNamespaceContext implements NamespaceContext {

    private HashMap<String, String> maps = new HashMap<>();

    protected SoapNamespaceContext() {/*
      maps.put("env", "http://www.w3.org/2003/05/soap-envelope");
      maps.put("S11", "http://schemas.xmlsoap.org/soap/envelope/");
      maps.put("eb", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/");
      maps.put("ns4", "http://www.w3.org/2000/09/xmldsig#");
      maps.put("ns5", "http://docs.oasis-open.org/ebxml-bp/ebbp-signals-2.0");
      maps.put("ns6", "http://www.w3.org/1999/xlink");
      maps.put("xmime", "http://www.w3.org/2005/05/xmlmime");
      maps.put("soap", "http://www.w3.org/2003/05/soap-envelope");
      maps.put("wsu", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
      maps.put("ds", "http://www.w3.org/2000/09/xmldsig#");
      //maps.put("wsse", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
      //maps.put("xenc", "http://www.w3.org/2001/04/xmlenc#");
      //maps.put("ec", "http://www.w3.org/2001/10/xml-exc-c14n#");

      //maps.put( "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "wsse");
      //maps.put("http://www.w3.org/2001/10/xml-exc-c14n#", "ec");
      //maps.put("http://www.w3.org/2001/04/xmlenc#", "xenc");
      maps.put("http://www.w3.org/2003/05/soap-envelope", "env");
      maps.put("http://schemas.xmlsoap.org/soap/envelope/", "S11");
      maps.put("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/", "eb");
      maps.put("http://www.w3.org/2000/09/xmldsig#", "ns4");
      maps.put("http://docs.oasis-open.org/ebxml-bp/ebbp-signals-2.0", "ns5");
      maps.put("http://www.w3.org/1999/xlink", "ns6");
      maps.put("http://www.w3.org/2005/05/xmlmime", "xmime");
      maps.put("http://www.w3.org/2003/05/soap-envelope", "soap");
      maps.put("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", "wsu");
      maps.put("http://www.w3.org/2000/09/xmldsig#", "ds");*/
    }

    @Override
    public String getNamespaceURI(String prefix) {
      return "*";
    }

    @Override
    public String getPrefix(String namespace) {
      return maps.get("namespace");
    }

    @Override
    public Iterator getPrefixes(String namespace) {
      return null;
    }
  }

  public static String describe(SOAPMessage message) {
    final StringBuilder sb = new StringBuilder();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    try {
      message.writeTo(baos);

      sb.append(new String(baos.toByteArray()));
    } catch (Exception e) {
      e.printStackTrace();
      sb.append("Error");
    }

    return sb.toString();
  }


  public static String readFile(String file) {
    try {
      FileInputStream fis = new FileInputStream(file);
      int len = fis.available();
      byte[] b = new byte[len];
      fis.read(b);
      fis.close();
      return new String(b);
    } catch (Exception ex) {
      return "";
    }
  }

  public static byte[] readBinaryFile(String file) {
    try {
      FileInputStream fis = new FileInputStream(file);
      int len = fis.available();
      byte[] b = new byte[len];
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      int read = -1;

      while ((read = fis.read(b, 0, b.length)) > 0) {
        baos.write(b, 0, read);
      }

      return baos.toByteArray();
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  private static Properties createProperty(String alias, String password) {
    String propertyString = "org.apache.wss4j.crypto.provider=minder.as4Utils.Merlin\n" +
       "org.apache.wss4j.crypto.merlin.keystore.type=jks\n" +
       "org.apache.wss4j.crypto.merlin.keystore.password=" + password + "\n" +
       "org.apache.wss4j.crypto.merlin.keystore.alias=" + alias + "\n" +
       "org.apache.wss4j.crypto.merlin.keystore.file=" + alias + "\n";

    Properties properties = new Properties();
    try {
      properties.load(new ByteArrayInputStream(propertyString.getBytes()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return properties;
  }

  private static void consumeResponseAttachments(SOAPMessage newMessage, List<Attachment> responseAttachments) throws IOException, SOAPException {
    for (Attachment at : responseAttachments) {
      InputStream is = at.getSourceStream();
      if (is.available() <= 0)
        continue;

      AttachmentPart ap = newMessage.createAttachmentPart();
      Map<String, String> headers = at.getHeaders();
      for (String key : headers.keySet()) {
        ap.addMimeHeader(key, headers.get(key));
      }

      if (ap.getContentId() == null)
        ap.setContentId("<" + at.getId() + ">");

      if (ap.getContentType() == null)
        ap.setContentType(at.getMimeType());

      int r = 0;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      while ((r = is.read()) != -1) {
        baos.write(r);
      }

      byte[] val = baos.toByteArray();
      ap.setRawContentBytes(val, 0, val.length, at.getMimeType());
      newMessage.addAttachmentPart(ap);
    }
  }


  private static void prepareAlgorithmsForEncryption(WSSecEncrypt encrypt, SOAPHeader previousHeader) throws XMLSecurityException {
    encrypt.setKeyIdentifierType(WSConstants.SKI_KEY_IDENTIFIER);
    //Body encryption removed as a requirement from esens test cases
    //encrypt.getParts().add(new WSEncryptionPart("Body", "http://www.w3.org/2003/05/soap-envelope", "Content"));

    switch (XMLENC_symmetricEncAlgorithm) {
      case E_SENS_DEFAULT:
        encrypt.setSymmetricEncAlgorithm(AES_128_GCM);
        break;

      case INHERIT:
        //use what ever was provided in the message
        useEncValuesInsecHdr4Data(encrypt, previousHeader);
        break;

      default:
        encrypt.setSymmetricEncAlgorithm(XMLENC_symmetricEncAlgorithm);
        break;
    }

    String keyEncAlg;
    switch (XMLENC_keyEncryptionAlgorithm) {
      case E_SENS_DEFAULT:
      case "":
        encrypt.setKeyEncAlgo(KEYTRANSPORT_RSAOAEP_XENC11);
        keyEncAlg = KEYTRANSPORT_RSAOAEP_XENC11;
        break;

      case INHERIT:
        keyEncAlg = useEncKeyEncAlgFrom(previousHeader, encrypt);
        break;

      default:
        encrypt.setKeyEncAlgo(XMLENC_keyEncryptionAlgorithm);
        keyEncAlg = XMLENC_keyEncryptionAlgorithm;
        break;
    }


    if (!keyEncAlg.equals(KEYTRANSPORT_RSA15)) {
      switch (XMLENC_digestAlgorithm) {
        case E_SENS_DEFAULT:
          encrypt.setDigestAlgorithm(SHA256);
          break;

        case INHERIT:
        case "":
          useEncKeyDigestAlgFrom(previousHeader, encrypt);
          break;

        default:
          encrypt.setDigestAlgorithm(XMLENC_digestAlgorithm);
          break;
      }
    }
  }

  private static void useEncValuesInsecHdr4Data(WSSecEncrypt encrypt, SOAPHeader soapHeader) throws XMLSecurityException {
    if (soapHeader == null) {
      throw new RuntimeException("Previous soap header must not be null if you use INHERIT for the parameters");
    }

    Element el = findSingleNode(soapHeader, "//:EncryptedData");
    XMLCipher cipher = XMLCipher.getInstance();
    cipher.init(XMLCipher.DECRYPT_MODE, null);
    EncryptedData encryptedData = cipher.loadEncryptedData(soapHeader.getOwnerDocument(), el);
    String algorithm = encryptedData.getEncryptionMethod().getAlgorithm();
    encrypt.setSymmetricEncAlgorithm(algorithm);
    LOG.debug("Symmetric Encryption Algoritm " + algorithm);
  }

  private static String useEncKeyEncAlgFrom(SOAPHeader soapHeader, WSSecEncrypt encrypt) throws XMLSecurityException {
    if (soapHeader == null) {
      throw new XMLSecurityException("Previous soap header must not be null if you use INHERIT for the parameters");
    }
    Element el = findSingleNode(soapHeader, "//:EncryptedKey");
    XMLCipher cipher = XMLCipher.getInstance();
    cipher.init(XMLCipher.DECRYPT_MODE, null);
    EncryptedKey encryptedKey = cipher.loadEncryptedKey(soapHeader.getOwnerDocument(), el);

    String algorithm = encryptedKey.getEncryptionMethod().getAlgorithm();
    encrypt.setKeyEncAlgo(algorithm);
    LOG.debug("Key Encryption Algoritm " + algorithm);
    return algorithm;
  }

  private static void useEncKeyDigestAlgFrom(SOAPHeader soapHeader, WSSecEncrypt encrypt) throws XMLSecurityException {
    if (soapHeader == null) {
      throw new XMLSecurityException("Previous soap header must not be null if you use INHERIT for the parameters");
    }
    Element el = findSingleNode(soapHeader, "//:EncryptedKey");
    XMLCipher cipher = XMLCipher.getInstance();
    cipher.init(XMLCipher.DECRYPT_MODE, null);
    EncryptedKey encryptedKey = cipher.loadEncryptedKey(soapHeader.getOwnerDocument(), el);

    String algorithm = encryptedKey.getEncryptionMethod().getDigestAlgorithm();
    if (algorithm != null && !algorithm.isEmpty())
      encrypt.setDigestAlgorithm(algorithm);
    LOG.debug("Key Digest Algoritm " + algorithm);
  }


  /**
   * Set the signature parameters (signature algorithm and digest method) from the previous security header
   *
   * @param signature
   * @param soapHeader
   */
  private static void useSgnValuesInsecHeader(WSSecSignature signature, SOAPHeader soapHeader) throws XMLSecurityException {
    if (soapHeader == null) {
      throw new RuntimeException("Previous soap header must not be null if you use INHERIT for the parameters");
    }

    Element el = findSingleNode(soapHeader, "//:Signature");
    XMLSignature xmlSignature = new XMLSignature(el, null);

    String signatureAlgorithm = xmlSignature.getSignedInfo().getSignatureAlgorithm().getURI();
    signature.setSignatureAlgorithm(signatureAlgorithm);

    //set the digest algortim WRT the signature algorithm
    if (RSA_SHA256.equals(signatureAlgorithm)) {
      signature.setDigestAlgo(SHA256);
    } else if (RSA_SHA1.equals(signatureAlgorithm)) {
      signature.setDigestAlgo(SHA1);
    } else if (DSA.equals(signatureAlgorithm)) {
      signature.setDigestAlgo(SHA1);
    } else {
      throw new XMLSecurityException("Unsupported signature algorithm " + signatureAlgorithm);
    }
  }


  private static void prepareAlgorithmsForSigning(SOAPHeader previousHeader, WSSecSignature signature) throws XMLSecurityException {
    switch (XMLSIG_signatureAlgorithm) {
      case E_SENS_DEFAULT:
        //use e-sens default values (http://wiki.ds.unipi.gr/display/ESENS/PR+-+AS4+-+1.11)
        signature.setSignatureAlgorithm(RSA_SHA256);
        signature.setDigestAlgo(SHA256);
        break;

      case INHERIT:
        //use what ever was provided in the message
        useSgnValuesInsecHeader(signature, previousHeader);
        break;

      default:
        signature.setSignatureAlgorithm(XMLSIG_signatureAlgorithm);
        if(XMLSIG_signatureAlgorithm.endsWith("sha256")) {
          signature.setDigestAlgo(SHA256);
        } else if(XMLSIG_signatureAlgorithm.endsWith("sha1")){
          signature.setDigestAlgo(SHA1);
        }
        break;
    }
    signature.setSigCanonicalization(XMLSIG_cannonicalizationAlgo);
    signature.setKeyIdentifierType(WSConstants.BST_DIRECT_REFERENCE);
  }
}

/*
    XMLCipher instance = XMLCipher.getInstance();
    instance.init(XMLCipher.DECRYPT_MODE, null);


    Element el = findSingleNode(message.getSOAPHeader(), "//:EncryptedKey");
    EncryptedKey encryptedKey = instance.loadEncryptedKey(el);
    el = findSingleNode(message.getSOAPHeader(), "//:EncryptedData");
    EncryptedData encryptedData = instance.loadEncryptedData(message.getSOAPHeader().getOwnerDocument(), el);


    EncryptionMethod encryptionMethod = encryptedKey.getEncryptionMethod();
    System.out.println(encryptionMethod.getAlgorithm());
    System.out.println(encryptionMethod.getDigestAlgorithm());
    System.out.println(encryptionMethod.getKeySize());
    System.out.println(encryptionMethod.getMGFAlgorithm());
    System.out.println(encryptionMethod.getOAEPparams());

    CipherData cipherData = encryptedData.getCipherData();
    String encoding = encryptedData.getEncoding();
    EncryptionMethod encryptionMethod1 = encryptedData.getEncryptionMethod();

    System.out.println(encryptionMethod1.getAlgorithm());
    System.out.println(encryptionMethod1.getDigestAlgorithm());
    System.out.println(encryptionMethod1.getKeySize());
    System.out.println(encryptionMethod1.getMGFAlgorithm());
    System.out.println(encryptionMethod1.getOAEPparams());

    KeyInfo keyInfo = encryptedData.getKeyInfo();
    System.out.println(keyInfo.containsX509Data());
    System.out.println(keyInfo.getX509Certificate());

    el = findSingleNode(message.getSOAPHeader(), "//:Signature");
    XMLSignature xmlSignature = new XMLSignature(el, null);

    System.out.println(prettyPrint(el));
    System.out.println(xmlSignature.getSignedInfo().getSignatureAlgorithm().getBaseLocalName());
    System.out.println(xmlSignature.getSignedInfo().getSignatureAlgorithm().getJCEAlgorithmString());
    System.out.println(xmlSignature.getSignedInfo().getSignatureAlgorithm().getJCEProviderName());
    System.out.println(xmlSignature.getSignedInfo().getSignatureAlgorithm().getURI());
    System.out.println(xmlSignature.getSignedInfo().getSignatureMethodURI());

 */