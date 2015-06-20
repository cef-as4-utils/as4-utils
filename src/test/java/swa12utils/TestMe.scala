import java.io.ByteArrayInputStream
import javax.xml.soap.{SOAPConstants, MessageFactory}

import minder.as4Utils.AS4Utils
import mtdl.MinderTdl

import scala.io.Source

object TestMe extends MinderTdl(Map[String, String](), false) {

  import minder.as4Utils.AS4Utils._

  import scala.collection.JavaConversions._

  /**
   * Intercept the as4 message, verify the signature and signature algorithms
   */
  def main(input: Array[String]): Unit = {
    val soapStr = Source.fromFile("soap3.xml").mkString.getBytes

    val soapMessage = AS4Utils.createMessage(null, new ByteArrayInputStream(soapStr))

    AS4Utils.init()

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