package com.dynamsoft.cameraxvideo

import kotlinx.serialization.Serializable

@Serializable
data class VideoModeResult {
    private var decodingResults = HashMap<Int,FrameDecodingResult>() //timestamp as key
    private var framesWithBarcodeFound = 0
    private var framesProcessed = 0
    private var firstBarcodeFoundPosition = -1
    private var firstFoundResult = ""

    fun setDecodingResults(results:HashMap<Int,FrameDecodingResult>){
        decodingResults = results
    }

    fun setFramesWithBarcodeFound(framesWithBarcodeFound:Int){
        this.framesWithBarcodeFound = framesWithBarcodeFound
    }

    fun setFirstBarcodeFoundPosition(firstBarcodeFoundPosition:Int){
        this.firstBarcodeFoundPosition = firstBarcodeFoundPosition
    }

    fun setFramesProcessed(framesProcessed:Int){
        this.framesProcessed = framesProcessed
    }

    fun setFirstFoundResult(firstFoundResult:String){
        this.firstFoundResult = firstFoundResult
    }
}