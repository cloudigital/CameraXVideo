package com.dynamsoft.cameraxvideo
import kotlinx.serialization.Serializable

@Serializable
class SDKResult (val framesModeResult: FramesModeResult,val videoModeResult: VideoModeResult)