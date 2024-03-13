package com.example.penguwidget.models

data class Pengu(
    val owner: Owner,
    val friend: Friend,
    val wallpaper: Wallpaper,
    val hungerSaturation: Int,
    val loveSaturation: Int,
    val previewImages: MutableList<PreviewImage>,
    val timelines: MutableList<Timeline>,
)

