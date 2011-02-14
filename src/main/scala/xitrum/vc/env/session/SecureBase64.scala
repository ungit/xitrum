package xitrum.vc.env.session

import java.security.SecureRandom
import javax.crypto.{Cipher, Mac}
import javax.crypto.spec.{SecretKeySpec, IvParameterSpec}
import xitrum.Config

/** See https://github.com/mmcgrana/ring/blob/master/ring-core/src/ring/middleware/session/cookie.clj */
object SecureBase64 {
  def serialize(value: Any): String = {
    val bytes = SeriDeseri.serialize(value)
    seal(key, bytes)
  }

  def deserialize(base64String: String): Option[Any] = {
    unseal(key, base64String) match {
      case None        => None
      case Some(bytes) => SeriDeseri.deserialize(bytes)
    }
  }

  //----------------------------------------------------------------------------

  private val key = Config.secureBase64Key.getBytes("UTF-8")

  // Algorithm to seed random numbers
  private val SEED_ALGORITHM = "SHA1PRNG"

  // Algorithm to generate a HMAC
  private val HMAC_ALGORITHM = "HmacSHA256"

  // Type of encryption to use
  private val CRYPT_TYPE = "AES"

  // Full algorithm to encrypt data with
  private val CRYPT_ALGORITHM = "AES/CBC/PKCS5Padding"

  /** @return a random byte array of the specified size. */
  private def secureRandomBytes(size: Int): Array[Byte] = {
    val seed = new Array[Byte](size)
    SecureRandom.getInstance(SEED_ALGORITHM).nextBytes(seed)
    seed
  }

  private def hmac(key: Array[Byte], data: Array[Byte]) = {
    val mac = Mac.getInstance(HMAC_ALGORITHM)
    mac.init(new SecretKeySpec(key, HMAC_ALGORITHM))
    mac.doFinal(data)
  }

  private def encrypt(key: Array[Byte], data: Array[Byte]): Array[Byte] = {
    val cipher    = Cipher.getInstance(CRYPT_ALGORITHM)
    val secretKey = new SecretKeySpec(key, CRYPT_TYPE)
    val iv        = secureRandomBytes(cipher.getBlockSize)

    cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv))
    iv ++ cipher.doFinal(data)
  }

  private def decrypt(key: Array[Byte], data: Array[Byte]): Array[Byte] = {
    val cipher      = Cipher.getInstance(CRYPT_ALGORITHM)
    val secretKey   = new SecretKeySpec(key, CRYPT_TYPE)
    val (iv, data2) = data.splitAt(cipher.getBlockSize)
    val ivSpec      = new IvParameterSpec(iv)

    cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
    cipher.doFinal(data2)
  }

  private def seal(key: Array[Byte], data: Array[Byte]): String = {
    val data2 = encrypt(key, data)
    Base64.encode(data2) + "--" + Base64.encode(hmac(key, data2))
  }

  private def unseal(key: Array[Byte], base64String: String): Option[Array[Byte]] = {
    try {
      val a = base64String.split("--")
      val base64Data = a(0)
      val base64hmac  = a(1)

      Base64.decode(base64Data) match {
        case None        => None
        case Some(data2) => if (base64hmac == Base64.encode(hmac(key, data2))) Some(decrypt(key, data2)) else None
      }
    } catch {
      case e => None
    }
  }
}
