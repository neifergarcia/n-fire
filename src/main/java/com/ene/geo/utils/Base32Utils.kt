package com.ene.geo.utils


class Base32Utils {

  /* number of bits per base 32 character */
  companion object {
    const val BITS_PER_BASE32_CHAR = 5
    private const val BASE32_CHARS = "0123456789bcdefghjkmnpqrstuvwxyz"

    fun valueToBase32Char(value: Int): Char {
      if (value < 0 || value >= BASE32_CHARS.length) {
        throw IllegalArgumentException("Not a valid base32 value: $value")
      }
      return BASE32_CHARS[value]
    }

    fun base32CharToValue(base32Char: Char): Int {
      val value = BASE32_CHARS.indexOf(base32Char)
      return if (value == -1) {
        throw IllegalArgumentException("Not a valid base32 char: $base32Char")
      } else {
        value
      }
    }

    fun isValidBase32String(string: String): Boolean {
      return string.matches("^[$BASE32_CHARS]*$".toRegex())
    }
  }
}