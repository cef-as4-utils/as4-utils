import java.io.ByteArrayInputStream
import javax.xml.soap.{SOAPConstants, MessageFactory}
import minder.as4Utils.SWA12Util

import scala.io.Source

object TestMe {

  import SWA12Util._

  import scala.collection.JavaConversions._

  /**
   * Intercept the as4 message, verify the signature and signature algorithms
   */
  def main(input: Array[String]): Unit = {
    val soapStr = Source.fromFile("samples/plain.txt").mkString.getBytes

    val soapMessage = SWA12Util.createMessage(null, new ByteArrayInputStream(soapStr))

    SWA12Util.init()

    val str = prettyPrint(soapMessage.getSOAPHeader)
    System.out.println("SOAP HEADER")
    System.out.println(str)
    System.out.println("========")

    val messageIdElement: org.w3c.dom.Element = findSingleNode(soapMessage.getSOAPPart, "//:MessageId");
    val tmp = messageIdElement.getTextContent()
    System.out.println("Received Message [" + tmp + "] from C2");

    //check the signature algorithm
    //val encryptionMethodElement: org.w3c.dom.Element = findSingleNode(soapMessage.getSOAPPart, "//:EncryptionMethod[last()]");
    val encryptionMethodElement: org.w3c.dom.Element = findSingleNode(soapMessage.getSOAPPart(), "//:EncryptedData/:EncryptionMethod");
    val encryptionAlgorithm = encryptionMethodElement.getAttribute("Algorithm")
    System.out.println("Encryption Algorithm: " + new String(encryptionAlgorithm))
  }

}