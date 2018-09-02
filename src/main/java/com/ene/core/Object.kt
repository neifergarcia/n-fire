package com.ene.core

import android.support.annotation.Keep
import java.util.*

@Keep
data class MetaData(
  var id: String = "",
  var createdAt: Long = Date().time,
  var user: String = ""
)

open class Object {
  val metadata: MetaData = MetaData()
}