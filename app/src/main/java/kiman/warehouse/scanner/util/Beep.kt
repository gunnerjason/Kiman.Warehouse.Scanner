package kiman.warehouse.scanner.util

import android.media.AudioManager
import android.media.ToneGenerator

object BeepPlayer {

    private var tone: ToneGenerator? = null

    fun play() {
        if (tone == null) {
            tone = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        }
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    }
}