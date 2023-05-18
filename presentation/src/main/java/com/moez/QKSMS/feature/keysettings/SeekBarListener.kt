package com.moez.QKSMS.feature.keysettings

import android.widget.SeekBar
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class SeekBarListener : SeekBar.OnSeekBarChangeListener {
    val posChanged: Subject<Int> = PublishSubject.create()

    override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
        if (fromUser) {
            posChanged.onNext(value)
        }
    }
    override fun onStartTrackingTouch(p0: SeekBar?) {}
    override fun onStopTrackingTouch(p0: SeekBar?) {}
}