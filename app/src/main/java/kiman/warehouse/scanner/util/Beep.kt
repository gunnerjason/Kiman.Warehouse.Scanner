package kiman.warehouse.scanner.util

import android.media.AudioManager
import android.media.ToneGenerator


private var toneGen: ToneGenerator? = null

fun playBeepOnce() {
    if (toneGen == null) toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
}
