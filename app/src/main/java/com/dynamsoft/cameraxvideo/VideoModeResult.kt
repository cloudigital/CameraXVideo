package com.dynamsoft.cameraxvideo

import kotlinx.serialization.Serializable

@Serializable
data class VideoModeResult(
    val decodingResults:HashMap<Int,FrameDecodingResult>,
    val framesWithBarcodeFound:Int,
    val framesProcessed:Int,
    val firstBarcodeFoundPosition:Int,
    val firstFoundResult:String
)