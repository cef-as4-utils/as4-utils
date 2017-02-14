package minder.as4Utils;

import org.apache.log4j.PropertyConfigurator;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.w3c.dom.Attr;

import javax.xml.soap.SOAPMessage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

import static minder.as4Utils.SWA12Util.*;

/**
 * Created by yerlibilgin on 25/05/15.
 */
public class TestCONFTEST18 {

  public static final String SAMPLE2_SOAP_MSG = readFile("samples/soap2.xml");

  private static final Object lock = new Object();

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
  public synchronized void testSignatureInheritSHA1() throws Exception {
    _testSignatureAlgorithm(RSA_SHA1, INHERIT);
  }

  @org.junit.Test
  public synchronized void testSignatureInheritSHA256() throws Exception {
    _testSignatureAlgorithm(RSA_SHA256, INHERIT);
  }

  @org.junit.Test
  public synchronized void testSignatureForceSHA1() throws Exception {
    _testSignatureAlgorithm(RSA_SHA256, RSA_SHA1);
  }

  @org.junit.Test
  public synchronized void testSignatureForceSHA256() throws Exception {
    _testSignatureAlgorithm(RSA_SHA1, RSA_SHA256);
  }

  @org.junit.Test
  public synchronized void testSignatureForceEsensDefaultFromSHA1() throws Exception {
    _testSignatureAlgorithm(RSA_SHA1, E_SENS_DEFAULT);
  }

  @org.junit.Test
  public synchronized void testSignatureForceEsensDefaultFromSHA256() throws Exception {
    _testSignatureAlgorithm(RSA_SHA256, E_SENS_DEFAULT);
  }

  @org.junit.Test
  public void testEncDataInheritAES128GCM() throws Exception {
    String first = AES_128_GCM;
    String second = INHERIT;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncData(plain, first, second);
  }

  @org.junit.Test
  public void testEncDataInheritAES128CBC() throws Exception {
    String first = AES_128;
    String second = INHERIT;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncData(plain, first, second);
  }

  @org.junit.Test
  public synchronized void testEncDataForceAES128CBC() throws Exception {
    String first = AES_128_GCM;
    String second = AES_128;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncData(plain, first, second);
  }

  @org.junit.Test
  public synchronized void testEncDataForceAES128GCM() throws Exception {
    String first = AES_128;
    String second = AES_128_GCM;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncData(plain, first, second);
  }

  @org.junit.Test
  public synchronized void testEncDataForceEsensDefaultFromAES128CBC() throws Exception {
    String first = AES_128;
    String second = E_SENS_DEFAULT;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncData(plain, first, second);
  }

  @org.junit.Test
  public synchronized void testEncDataForceEsensDefaultFromAES128GCM() throws Exception {
    String first = AES_128_GCM;
    String second = E_SENS_DEFAULT;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncData(plain, first, second);
  }

  @org.junit.Test
  public synchronized void testEncKeyAlgorithmInheritRSAOAEP() throws Exception {
    String first = KEYTRANSPORT_RSAOAEP;
    String second = INHERIT;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncKeyAlgorithm(plain, first, second);
  }

  @org.junit.Test
  public synchronized void testEncKeyAlgorithmInheritRSAOAEP_XENC11() throws Exception {
    String first = KEYTRANSPORT_RSAOAEP_XENC11;
    String second = INHERIT;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncKeyAlgorithm(plain, first, second);
  }

  @org.junit.Test
  public synchronized void testEncKeyAlgorithmForceRSAOAEP() throws Exception {
    String first = KEYTRANSPORT_RSAOAEP;
    String second = KEYTRANSPORT_RSAOAEP_XENC11;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncKeyAlgorithm(plain, first, second);
  }

  @org.junit.Test
  public synchronized void testEncKeyAlgorithmForceRSAOAEP_XENC11() throws Exception {
    String first = KEYTRANSPORT_RSAOAEP;
    String second = KEYTRANSPORT_RSAOAEP_XENC11;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncKeyAlgorithm(plain, first, second);
  }

  @org.junit.Test
  public synchronized void testEncKeyAlgorithmForceEsensDefaultFromRSAOAEP() throws Exception {
    String first = KEYTRANSPORT_RSAOAEP;
    String second = E_SENS_DEFAULT;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncKeyAlgorithm(plain, first, second);
  }

  @org.junit.Test
  public synchronized void testEncKeyAlgorithmForceEsensDefaultFromRSAOAEP_XENC11() throws Exception {
    String first = KEYTRANSPORT_RSAOAEP_XENC11;
    String second = E_SENS_DEFAULT;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncKeyAlgorithm(plain, first, second);
  }

  @org.junit.Test
  public synchronized void testEncKeyDigestInheritSHA256() throws Exception {
    String first = SHA256;
    String second = INHERIT;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncKeyDigestAlgorithm(plain, first, second);
  }

  @org.junit.Test
  public synchronized void testEncKeyDigestInheritSHA1() throws Exception {
    String first = SHA1;
    String second = INHERIT;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncKeyDigestAlgorithm(plain, first, second);
  }

  @org.junit.Test
  public synchronized void testEncKeyDigestForceSHA256() throws Exception {
    String first = SHA1;
    String second = SHA256;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncKeyDigestAlgorithm(plain, first, second);
  }

  @org.junit.Test
  public synchronized void testEncKeyDigestForceSHA1() throws Exception {
    String first = SHA256;
    String second = SHA1;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncKeyDigestAlgorithm(plain, first, second);
  }

  @org.junit.Test
  public synchronized void testEncKeyDigestForceEsensDefaultFromSHA1() throws Exception {
    String first = SHA1;
    String second = E_SENS_DEFAULT;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncKeyDigestAlgorithm(plain, first, second);
  }

  @org.junit.Test
  public synchronized void testEncKeyDigestForceEsensDefaultFromSHA256() throws Exception {
    String first = SHA256;
    String second = E_SENS_DEFAULT;
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));
    _testEncKeyDigestAlgorithm(plain, first, second);
  }

  private synchronized void _testSignatureAlgorithm(String first, String second) throws Exception {
    SOAPMessage plain = deserializeSOAPMessage(new FileInputStream("samples/plain.txt"));

    XMLSIG_signatureAlgorithm = first;

    SOAPMessage message = signAndEncrypt((SOAPMessage) plain, (Corner) Corner.CORNER_2);
    FileOutputStream fos = new FileOutputStream("encrypted.xml");
    fos.write(prettyPrint(message.getSOAPPart()).getBytes());
    fos.close();

    verifyAndDecrypt(message, Corner.CORNER_3);

    XMLSIG_signatureAlgorithm = second;

    SOAPMessage message2 = signAndEncrypt((SOAPMessage) plain, (Corner) Corner.CORNER_2, message.getSOAPHeader());
    FileOutputStream fos2 = new FileOutputStream("encrypted2.xml");
    fos2.write(prettyPrint(message2.getSOAPPart()).getBytes());
    fos2.close();
    verifyAndDecrypt(message2, Corner.CORNER_3);


    Attr signatureMethod1 = findSingleNode(message.getSOAPHeader(), "//:SignatureMethod/@Algorithm");
    Attr signatureMethod2 = findSingleNode(message2.getSOAPHeader(), "//:SignatureMethod/@Algorithm");


    String second1 = second;
    if (second1.equals(INHERIT)) {
      second1 = first;
    } else if (second1.equals(E_SENS_DEFAULT))
      second1 = RSA_SHA256;

    Assert.assertEquals(first, signatureMethod1.getValue());
    Assert.assertEquals(second1, signatureMethod2.getValue());


    List<Attr> objects1 = listNodes(message.getSOAPHeader(), "//:Reference/:DigestMethod/@Algorithm");
    List<Attr> objects2 = listNodes(message2.getSOAPHeader(), "//:Reference/:DigestMethod/@Algorithm");

    for (Attr obj : objects1) {
      System.out.println(obj.getValue());
    }
    System.out.println();
    for (Attr obj : objects2) {
      System.out.println(obj.getValue());
    }

    Assert.assertTrue("The sizes must be the same", objects1.size() == objects2.size());

    String firstDigest = SHA256;

    if (first.endsWith("sha1")) {
      firstDigest = SHA1;
    } else if (first.endsWith("sha256")) {
      firstDigest = SHA256;
    }

    String secondDigest = second;
    if (secondDigest.equals(INHERIT)) {
      if (first.endsWith("sha1")) {
        secondDigest = SHA1;
      } else if (first.endsWith("sha256")) {
        secondDigest = SHA256;
      }
    } else if (secondDigest.equals(E_SENS_DEFAULT))
      secondDigest = SHA256;
    else if (second.endsWith("sha1")) {
      secondDigest = SHA1;
    } else if (second.endsWith("sha256")) {
      secondDigest = SHA256;
    }


    for (int i = 0; i < objects1.size(); ++i) {
      Assert.assertEquals("Digest methods should be the same", firstDigest, objects1.get(i).getValue());
    }

    for (int i = 0; i < objects2.size(); ++i) {
      Assert.assertEquals("Digest methods should be the same", secondDigest, objects2.get(i).getValue());
    }
  }

  private void _testEncData(SOAPMessage plain, String first, String second) throws Exception {
    XMLENC_symmetricEncAlgorithm = first;

    SOAPMessage message = signAndEncrypt(plain, Corner.CORNER_2);
    FileOutputStream fos = new FileOutputStream("encrypted.xml");
    fos.write(prettyPrint(message.getSOAPPart()).getBytes());
    fos.close();

    verifyAndDecrypt(message, Corner.CORNER_3);

    XMLENC_symmetricEncAlgorithm = second;

    SOAPMessage message2 = signAndEncrypt(plain, Corner.CORNER_2, message.getSOAPHeader());
    FileOutputStream fos2 = new FileOutputStream("encrypted2.xml");
    fos2.write(prettyPrint(message2.getSOAPPart()).getBytes());
    fos2.close();
    verifyAndDecrypt(message2, Corner.CORNER_3);

    List<Attr> objects1 = listNodes(message.getSOAPHeader(), "//:EncryptedData/:EncryptionMethod/@Algorithm");
    List<Attr> objects2 = listNodes(message2.getSOAPHeader(), "//:EncryptedData/:EncryptionMethod/@Algorithm");

    for (Attr obj : objects1) {
      System.out.println(obj.getValue());
    }
    System.out.println();
    for (Attr obj : objects2) {
      System.out.println(obj.getValue());
    }

    Assert.assertTrue("The sizes must be the same", objects1.size() == objects2.size());

    if (second.equals(INHERIT))
      second = first;

    if (second.equals(E_SENS_DEFAULT))
      second = AES_128_GCM;


    for (int i = 0; i < objects1.size(); ++i) {
      Assert.assertEquals("EncryptionMethod should be the same", first, objects1.get(i).getValue());
    }

    for (int i = 0; i < objects2.size(); ++i) {
      Assert.assertEquals("EncryptionMethod should be the same", second, objects2.get(i).getValue());
    }
  }

  private void _testEncKeyDigestAlgorithm(SOAPMessage plain, String first, String second) throws Exception {
    XMLENC_digestAlgorithm = first;

    SOAPMessage message = signAndEncrypt(plain, Corner.CORNER_2);
    FileOutputStream fos = new FileOutputStream("encrypted.xml");
    fos.write(prettyPrint(message.getSOAPPart()).getBytes());
    fos.close();

    verifyAndDecrypt(message, Corner.CORNER_3);

    XMLENC_digestAlgorithm = second;

    SOAPMessage message2 = signAndEncrypt(plain, Corner.CORNER_2, message.getSOAPHeader());
    FileOutputStream fos2 = new FileOutputStream("encrypted2.xml");
    fos2.write(prettyPrint(message2.getSOAPPart()).getBytes());
    fos2.close();
    verifyAndDecrypt(message2, Corner.CORNER_3);

    Attr attr1 = findSingleNode(message.getSOAPHeader(), "//:EncryptedKey/:EncryptionMethod/:DigestMethod/@Algorithm");
    Attr attr2 = findSingleNode(message2.getSOAPHeader(), "//:EncryptedKey/:EncryptionMethod/:DigestMethod/@Algorithm");

    System.out.println(attr1.getValue());
    System.out.println(attr2.getValue());


    if (second.equals(INHERIT))
      second = first;

    if (second.equals(E_SENS_DEFAULT))
      second = SHA256;


    Assert.assertEquals("EncryptionMethod should be the same", first, attr1.getValue());

    Assert.assertEquals("EncryptionMethod should be the same", second, attr2.getValue());
  }

  private void _testEncKeyAlgorithm(SOAPMessage plain, String first, String second) throws Exception {
    XMLENC_keyEncryptionAlgorithm = first;

    SOAPMessage message = signAndEncrypt(plain, Corner.CORNER_2);
    FileOutputStream fos = new FileOutputStream("encrypted.xml");
    fos.write(prettyPrint(message.getSOAPPart()).getBytes());
    fos.close();

    verifyAndDecrypt(message, Corner.CORNER_3);

    XMLENC_keyEncryptionAlgorithm = second;

    SOAPMessage message2 = signAndEncrypt(plain, Corner.CORNER_2, message.getSOAPHeader());
    FileOutputStream fos2 = new FileOutputStream("encrypted2.xml");
    fos2.write(prettyPrint(message2.getSOAPPart()).getBytes());
    fos2.close();
    verifyAndDecrypt(message2, Corner.CORNER_3);

    Attr attr1 = findSingleNode(message.getSOAPHeader(), "//:EncryptedKey/:EncryptionMethod/@Algorithm");
    Attr attr2 = findSingleNode(message2.getSOAPHeader(), "//:EncryptedKey/:EncryptionMethod/@Algorithm");

    System.out.println(attr1.getValue());
    System.out.println(attr2.getValue());

    if (second.equals(INHERIT))
      second = first;

    if (second.equals(E_SENS_DEFAULT))
      second = KEYTRANSPORT_RSAOAEP_XENC11;


    Assert.assertEquals("EncryptionMethod should be the same", first, attr1.getValue());

    Assert.assertEquals("EncryptionMethod should be the same", second, attr2.getValue());
  }

}
