package com.pankaj.smartcapture

import android.net.Uri

data class ImageModel(
  val id: Long,
  val displayName: String,
  val contentUri: Uri
)
