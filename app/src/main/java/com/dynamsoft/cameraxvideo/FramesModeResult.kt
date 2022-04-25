package com.dynamsoft.cameraxvideo

class FramesModeResult {
    private var decodingResults = ArrayList<FrameDecodingResult>()
    private var framesWithBarcodeFound = 0
    private var framesProcessed = 0
    private var firstDecodedFrameIndex = -1
    private var firstFoundResult = ""

    fun setDecodingResults(results:ArrayList<FrameDecodingResult>){
        decodingResults = results
    }

    fun setFramesWithBarcodeFound(framesWithBarcodeFound:Int){
        this.framesWithBarcodeFound = framesWithBarcodeFound
    }

    fun setFirstDecodedFrameIndex(firstDecodedFrameIndex:Int){
        this.firstDecodedFrameIndex = firstDecodedFrameIndex
    }

    fun setFramesProcessed(framesProcessed:Int){
        this.framesProcessed = framesProcessed
    }

    fun setFirstFoundResult(firstFoundResult:String){
        this.firstFoundResult = firstFoundResult
    }
}