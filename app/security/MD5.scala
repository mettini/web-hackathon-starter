package security

import play.api._
import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex

object MD5 {

  def md5Hex(s: String): String = {
    val hash = MessageDigest.getInstance("MD5").digest(s.getBytes("CP1252"))
    new String(Hex.encodeHex(hash))
  }
}
