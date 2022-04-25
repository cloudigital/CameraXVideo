package com.dynamsoft.cameraxvideo
import kotlinx.serialization.Serializable

@Serializable
class FramesModeResult (
    val decodingResults:ArrayList<FrameDecodingResult>,
    val framesWithBarcodeFound:Int,
    val framesProcessed:Int,
    val firstDecodedFrameIndex:Int,
    val firstFoundResult:String
)