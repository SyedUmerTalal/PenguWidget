package com.example.penguwidget.models

data class Timeline(
    val hoursIntoFuture: Int,
    val hungerSaturation: Int,
    val loveSaturation: Int,
    val previewImages: MutableList<PreviewImage>,
)