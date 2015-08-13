package minder.as4Utils;

import org.apache.log4j.PropertyConfigurator;
import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.util.AttachmentUtils;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSSConfig;
import org.apache.wss4j.dom.WSSecurityEngine;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.w3c.dom.*;

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
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by yerlibilgin on 13/05/15.
 */
public class AS4Utils {
  public static final String signatureAlgorithm = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
  public static final int encKeyIdentifierType = WSConstants.SKI_KEY_IDENTIFIER;
  public static final String symmetricEncAlgorithm = WSConstants.AES_128_GCM;
  public static final int signKeyIdentifierType = WSConstants.BST_DIRECT_REFERENCE;
  public static final String digestAlgorithm = WSConstants.SHA256;

  static MessageFactory messageFactory = null;
  static SOAPConnectionFactory soapConnectionFactory = null;

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AS4Utils.class);
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
   * 1 - A LinkedHashMap containing header name and header values fields. (for mime headers)
   * 2 - the byte array of the SOAPMessage
   *
   * @param headers
   * @param message
   * @return
   */
  public static byte[] serializeSOAPMessage(MimeHeaders headers, SOAPMessage message) {
    LinkedHashMap<String, String[]> headerMap = new LinkedHashMap<>();
    Iterator iterator = headers.getAllHeaders();

    while (iterator.hasNext()) {
      MimeHeader header = (MimeHeader) iterator.next();

      String[] values = headers.getHeader(header.getName());
      headerMap.put(header.getName(), values);
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      message.writeTo(baos);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Object[] toSerialize = new Object[]{headerMap, baos.toByteArray()};

    baos = new ByteArrayOutputStream();
    try {
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(toSerialize);
      oos.close();
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

  public static SOAPMessage deserializeSOAPMessage(InputStream is) {
    return deserializeSOAPMessage(is, false);
  }

  public static SOAPMessage deserializeSOAPMessage(InputStream is, boolean close) {
    try {
      ObjectInputStream ois = new ObjectInputStream(is);
      Object[] objects = (Object[]) ois.readObject();

      HashMap<String, String[]> hashMap = (HashMap<String, String[]>) objects[0];
      MimeHeaders headers = new MimeHeaders();
      for (String name : hashMap.keySet()) {
        //first item is key, second is value
        for (String value : hashMap.get(name)) {
          headers.addHeader(name, value);
        }
      }

      //object[1] has been created by the writeTo method of SOAPMessage, so do the inverse.
      return messageFactory.createMessage(headers, new ByteArrayInputStream((byte[]) objects[1]));
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    } finally {
      try {
        is.close();
      } catch (Exception ex) {
      }
    }
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
    InputStream is = AS4Utils.class.getResourceAsStream(s);
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
    return new ByteArrayInputStream(buf);
  }

  //region Extract Attachments
  public static SOAPMessage verifyAndDecrypt(SOAPMessage message, Corner receiver) throws Exception {
    message.saveChanges();
    SOAPMessage newMessage = deserializeSOAPMessage(serializeSOAPMessage(message.getMimeHeaders(), message));
    newMessage.removeAllAttachments();

    stripWSSEC(newMessage.getSOAPHeader());

    List<Attachment> encryptedAttachments = new ArrayList<>();
    message.getAttachments().forEachRemaining(new Consumer<AttachmentPart>() {
      @Override
      public void accept(AttachmentPart o) {
        final Attachment at = new Attachment();
        o.getAllMimeHeaders().forEachRemaining(new Consumer<MimeHeader>() {
          @Override
          public void accept(MimeHeader mimeHeader) {
            at.addHeader(mimeHeader.getName(), mimeHeader.getValue());
          }
        });

        at.setMimeType(o.getContentType());
        at.setId(o.getContentId().replaceAll(">|<", ""));
        try {
          at.setSourceStream(new ByteArrayInputStream(o.getRawContentBytes()));
        } catch (SOAPException e) {
          e.printStackTrace();
        }
        encryptedAttachments.add(at);
      }
    });

    AttachmentCallbackHandler attachmentCallbackHandler = new AttachmentCallbackHandler(encryptedAttachments);
    verify(message.getSOAPPart(), attachmentCallbackHandler, receiver);

    for (Attachment at : attachmentCallbackHandler.getResponseAttachments()) {
      InputStream is = at.getSourceStream();
      if (is.available() <= 0)
        continue;

      AttachmentPart ap = newMessage.createAttachmentPart();
      Map<String, String> headers = at.getHeaders();
      for (String key : headers.keySet()) {
        ap.addMimeHeader(key, headers.get(key));
      }

      ap.setContentId(at.getId());
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
    requestData.setAttachmentCallbackHandler(attachmentCallbackHandler);
    requestData.setSigVerCrypto(trustCrypto);
    requestData.setDecCrypto(receiver == Corner.CORNER_2 ? c2Crypto : c3Crypto);
    requestData.setCallbackHandler(new KeystoreCallbackHandler());
    return secEngine.processSecurityHeader(doc, requestData);
  }

  //endregion

  /**
   * Encrypts and Signs the soap message together with the attachments.
   * <p>
   * Strips out the ws:security header if any.
   * <p>
   * Assumes that the attachments are already decrypted.
   *
   * @param message
   */
  public static SOAPMessage encryptAndSign(SOAPMessage message, Corner sender) {
    try {
      message.saveChanges();
      SOAPMessage newMessage = deserializeSOAPMessage(serializeSOAPMessage(message.getMimeHeaders(), message));
      newMessage.removeAllAttachments();
      stripWSSEC(newMessage.getSOAPHeader());
      Document doc = newMessage.getSOAPPart();

      WSSecHeader secHeader = new WSSecHeader();
      secHeader.insertSecurityHeader(doc);

      WSSecEncrypt encrypt = new WSSecEncrypt();
      //if C2 is sending, then enrypt with C3 certificate.
      encrypt.setUserInfo(sender == Corner.CORNER_2 ? USER_C3 : USER_C2, sender == Corner.CORNER_2 ? PWD_C3 : PWD_C2);
      encrypt.getParts().add(new WSEncryptionPart("cid:Attachments", "Element"));
      encrypt.setKeyIdentifierType(encKeyIdentifierType);
      encrypt.setSymmetricEncAlgorithm(symmetricEncAlgorithm);

      final List<Attachment> attachments = new ArrayList<>();
      message.getAttachments().forEachRemaining(new Consumer<AttachmentPart>() {
        @Override
        public void accept(final AttachmentPart o) {
          final Attachment attachment = new Attachment();
          attachment.setMimeType(o.getContentType());
          o.getAllMimeHeaders().forEachRemaining(new Consumer<MimeHeader>() {
            @Override
            public void accept(MimeHeader mime) {
              attachment.addHeader(mime.getName(), mime.getValue());
            }
          });
          attachment.setId(o.getContentId());
          try {
            attachment.setSourceStream(new ByteArrayInputStream(o.getRawContentBytes()));
          } catch (SOAPException e) {
            throw new RuntimeException(e);
          }

          attachments.add(attachment);
        }
      });

      AttachmentCallbackHandler attachmentCallbackHandler = new AttachmentCallbackHandler(attachments);
      encrypt.setAttachmentCallbackHandler(attachmentCallbackHandler);
      //if C2 is sending, then enrypt with C3 certificate.
      doc = encrypt.build(doc, sender == Corner.CORNER_2 ? c3Crypto : c2Crypto, secHeader);

      WSSecSignature signature = new WSSecSignature();
      //if C2 is sending, then sign with C2 key.
      signature.setUserInfo(sender == Corner.CORNER_2 ? USER_C2 : USER_C3, sender == Corner.CORNER_2 ? PWD_C2 : PWD_C3);
      signature.getParts().add(new WSEncryptionPart("Messaging", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/", "Content"));
      signature.getParts().add(new WSEncryptionPart("Body", "http://www.w3.org/2003/05/soap-envelope", "Element"));
      signature.getParts().add(new WSEncryptionPart("cid:Attachments", "Content"));
      signature.setDigestAlgo(digestAlgorithm);
      signature.setSignatureAlgorithm(signatureAlgorithm);
      signature.setKeyIdentifierType(signKeyIdentifierType);

      List<Attachment> encryptedAttachments = attachmentCallbackHandler.getResponseAttachments();
      attachmentCallbackHandler = new AttachmentCallbackHandler(encryptedAttachments);
      signature.setAttachmentCallbackHandler(attachmentCallbackHandler);

      //if C2 is sending, then sign with C2 key.
      doc = signature.build(doc, sender == Corner.CORNER_2 ? c2Crypto : c3Crypto, secHeader);
      if (LOG.isDebugEnabled()) {
        String outputString = XMLUtils.PrettyDocumentToString(doc);
        LOG.debug(outputString);
      }

      for (Attachment at : attachmentCallbackHandler.getResponseAttachments()) {
        System.out.println("ATID " + at.getId());
        InputStream is = at.getSourceStream();
        if (is.available() <= 0)
          continue;

        AttachmentPart ap = newMessage.createAttachmentPart();
        Map<String, String> headers = at.getHeaders();
        for (String key : headers.keySet()) {
          ap.addMimeHeader(key, headers.get(key));
        }

        ap.setContentId(at.getId());
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

      newMessage.saveChanges();
      return newMessage;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Signs the soap message (no encrpytion)
   * <p>
   * Strips out the ws:security header if any.
   * <p>
   * Assumes that the attachments are already decrypted.
   *
   * @param message
   */
  public static SOAPMessage sign(SOAPMessage message, Corner sender) {
    try {
      message.saveChanges();
      SOAPMessage newMessage = deserializeSOAPMessage(serializeSOAPMessage(message.getMimeHeaders(), message));
      newMessage.removeAllAttachments();
      stripWSSEC(newMessage.getSOAPHeader());
      Document doc = newMessage.getSOAPPart();

      WSSecHeader secHeader = new WSSecHeader();
      secHeader.insertSecurityHeader(doc);

      final List<Attachment> attachments = new ArrayList<>();
      message.getAttachments().forEachRemaining(new Consumer<AttachmentPart>() {
        @Override
        public void accept(final AttachmentPart o) {
          final Attachment attachment = new Attachment();
          attachment.setMimeType(o.getContentType());
          o.getAllMimeHeaders().forEachRemaining(new Consumer<MimeHeader>() {
            @Override
            public void accept(MimeHeader mime) {
              attachment.addHeader(mime.getName(), mime.getValue());
            }
          });
          attachment.setId(o.getContentId());
          try {
            attachment.setSourceStream(new ByteArrayInputStream(o.getRawContentBytes()));
          } catch (SOAPException e) {
            throw new RuntimeException(e);
          }

          attachments.add(attachment);
        }
      });

      WSSecSignature signature = new WSSecSignature();
      //if C2 is sending, then sign with C2 key.
      signature.setUserInfo(sender == Corner.CORNER_2 ? USER_C2 : USER_C3, sender == Corner.CORNER_2 ? PWD_C2 : PWD_C3);
      signature.getParts().add(new WSEncryptionPart("Messaging", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/", "Content"));
      signature.getParts().add(new WSEncryptionPart("Body", "http://www.w3.org/2003/05/soap-envelope", "Element"));
      signature.getParts().add(new WSEncryptionPart("cid:Attachments", "Content"));
      signature.setDigestAlgo(digestAlgorithm);
      signature.setSignatureAlgorithm(signatureAlgorithm);
      signature.setKeyIdentifierType(signKeyIdentifierType);

      AttachmentCallbackHandler attachmentCallbackHandler = new AttachmentCallbackHandler(attachments);
      signature.setAttachmentCallbackHandler(attachmentCallbackHandler);

      //if C2 is sending, then sign with C2 key.
      doc = signature.build(doc, sender == Corner.CORNER_2 ? c2Crypto : c3Crypto, secHeader);
      if (LOG.isDebugEnabled()) {
        String outputString = XMLUtils.PrettyDocumentToString(doc);
        LOG.debug(outputString);
      }

      for (Attachment at : attachmentCallbackHandler.getResponseAttachments()) {
        System.out.println("ATID " + at.getId());
        InputStream is = at.getSourceStream();
        if (is.available() <= 0)
          continue;

        AttachmentPart ap = newMessage.createAttachmentPart();
        Map<String, String> headers = at.getHeaders();
        for (String key : headers.keySet()) {
          ap.addMimeHeader(key, headers.get(key));
        }

        ap.setContentId(at.getId());
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
   * @param message
   */
  public static SOAPMessage signAndEncrypt(SOAPMessage message, Corner sender) {
    try {
      message.saveChanges();
      SOAPMessage newMessage = deserializeSOAPMessage(serializeSOAPMessage(message.getMimeHeaders(), message));
      newMessage.removeAllAttachments();
      stripWSSEC(newMessage.getSOAPHeader());
      Document doc = newMessage.getSOAPPart();

      WSSecHeader secHeader = new WSSecHeader();
      secHeader.insertSecurityHeader(doc);

      WSSecSignature signature = new WSSecSignature();
      //if C2 is sending, then sign with C2 key.
      signature.setUserInfo(sender == Corner.CORNER_2 ? USER_C2 : USER_C3, sender == Corner.CORNER_2 ? PWD_C2 : PWD_C3);
      signature.getParts().add(new WSEncryptionPart("Messaging", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/", "Content"));
      signature.getParts().add(new WSEncryptionPart("Body", "http://www.w3.org/2003/05/soap-envelope", "Element"));

      signature.setDigestAlgo(digestAlgorithm);
      signature.setSignatureAlgorithm(signatureAlgorithm);
      signature.setKeyIdentifierType(signKeyIdentifierType);


      AttachmentCallbackHandler attachmentCallbackHandler = null;
      final int attachmentCount = message.countAttachments();
      if (attachmentCount > 0) {
        signature.getParts().add(new WSEncryptionPart("cid:Attachments", "Content"));
        final List<Attachment> attachments = new ArrayList<>();
        message.getAttachments().forEachRemaining(new Consumer<AttachmentPart>() {
          @Override
          public void accept(final AttachmentPart o) {
            final Attachment attachment = new Attachment();
            attachment.setMimeType(o.getContentType());
            o.getAllMimeHeaders().forEachRemaining(new Consumer<MimeHeader>() {
              @Override
              public void accept(MimeHeader mime) {
                attachment.addHeader(mime.getName(), mime.getValue());
              }
            });
            attachment.setId(o.getContentId());
            try {
              attachment.setSourceStream(new ByteArrayInputStream(o.getRawContentBytes()));
            } catch (SOAPException e) {
              throw new RuntimeException(e);
            }

            attachments.add(attachment);
          }
        });
        attachmentCallbackHandler = new AttachmentCallbackHandler(attachments);
        signature.setAttachmentCallbackHandler(attachmentCallbackHandler);
      }

      //if C2 is sending, then sign with C2 key.
      doc = signature.build(doc, sender == Corner.CORNER_2 ? c2Crypto : c3Crypto, secHeader);
      if (LOG.isDebugEnabled()) {
        String outputString = XMLUtils.PrettyDocumentToString(doc);
        LOG.debug(outputString);
      }


      WSSecEncrypt encrypt = new WSSecEncrypt();
      //if C2 is sending, then enrypt with C3 certificate.
      encrypt.setUserInfo(sender == Corner.CORNER_2 ? USER_C3 : USER_C2, sender == Corner.CORNER_2 ? PWD_C3 : PWD_C2);
      encrypt.setKeyIdentifierType(encKeyIdentifierType);
      encrypt.setSymmetricEncAlgorithm(symmetricEncAlgorithm);

      if (attachmentCount > 0) {
        encrypt.getParts().add(new WSEncryptionPart("cid:Attachments", "Element"));
        List<Attachment> signedAttachments = attachmentCallbackHandler.getResponseAttachments();
        attachmentCallbackHandler = new AttachmentCallbackHandler(signedAttachments);
        encrypt.setAttachmentCallbackHandler(attachmentCallbackHandler);
      }


      //if C2 is sending, then enrypt with C3 certificate.
      doc = encrypt.build(doc, sender == Corner.CORNER_2 ? c3Crypto : c2Crypto, secHeader);

      if (attachmentCount > 0) {
        for (Attachment at : attachmentCallbackHandler.getResponseAttachments()) {
          System.out.println("ATID " + at.getId());
          InputStream is = at.getSourceStream();
          if (is.available() <= 0)
            continue;

          AttachmentPart ap = newMessage.createAttachmentPart();
          Map<String, String> headers = at.getHeaders();
          for (String key : headers.keySet()) {
            ap.addMimeHeader(key, headers.get(key));
          }

          ap.setContentId(at.getId());
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
      newMessage.saveChanges();
      return newMessage;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Removes the ws:security element from the given node.
   *
   * @param element
   * @return
   */
  public static NodeList stripWSSEC(Element element) {
    NodeList elements = element.getElementsByTagNameNS("*", "Security");

    for (int i = 0; i < elements.getLength(); ++i) {
      org.w3c.dom.Element item = (Element) elements.item(i);
      if (item.getParentNode() != null)
        item.getParentNode().removeChild(item);
    }
    return elements;
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
      return null;
    }

    @Override
    public Iterator getPrefixes(String namespace) {
      return null;
    }
  }

  public static String describe(SOAPMessage message) {
    final StringBuilder sb = new StringBuilder();
    sb.append("______________________________________________________\n");
    sb.append(prettyPrint(message.getSOAPPart())).append('\n');

    sb.append("-------------------\n\n");
    message.getAttachments().forEachRemaining(new Consumer<AttachmentPart>() {
      @Override
      public void accept(AttachmentPart attachmentPart) {
        sb.append("Attachment: ").append(attachmentPart.getContentId()).append('\n');
        attachmentPart.getAllMimeHeaders().forEachRemaining(new Consumer<MimeHeader>() {
          @Override
          public void accept(MimeHeader o) {
            sb.append("\t").append(o.getName()).append(" ---> ").append(o.getValue()).append('\n');
          }
        });

        try {
          sb.append(new String(attachmentPart.getRawContentBytes()));
        } catch (SOAPException e) {
          throw new RuntimeException();
        }
        sb.append("-------------------\n\n");
      }
    });
    sb.append("______________________________________________________\n");

    try {
      message.saveChanges();
    } catch (SOAPException e) {
      e.printStackTrace();
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


}

