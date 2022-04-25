package com.dynamsoft.cameraxvideo

class FrameDecodingResult(results:ArrayList<String>,timeSpent:Long) {
    private var results = ArrayList<String>()
    private var timeSpent:Long = 0

    init {
        this.results = results
        this.timeSpent = timeSpent
    }
}