package com.ene.core

import java.util.*

data class MetaData(
  var id: String = "",
  var createdAt: Long = Date().time,
  var user: String = ""
)

open class Object {
  val metadata: MetaData = MetaData()
}