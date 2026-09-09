package com.localflow.player.playback

data class VideoPolicy(val disableVideo: Boolean, val pause: Boolean)
fun videoPolicy(isVideo: Boolean, visible: Boolean, audioOnly: Boolean, allowBackground: Boolean): VideoPolicy =
    VideoPolicy(isVideo && (audioOnly || !visible),isVideo && !visible && !allowBackground)
