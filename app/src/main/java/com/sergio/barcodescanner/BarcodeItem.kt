package com.sergio.barcodescanner

data class BarcodeItem(
    val id: String = System.currentTimeMillis().toString(),
    val code: String,
    val imagePath: String? = null,
    var isSelected: Boolean = true
)

enum class SelectAllState { Unchecked, Indeterminate, Checked }
