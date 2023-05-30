package com.moez.QKSMS.feature.keysettings

import android.text.Editable
import android.text.TextWatcher
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.Timer
import java.util.TimerTask


internal class KeyTextWatcher : TextWatcher {
    val keyChanged: Subject<String> = PublishSubject.create()
    private var timer = Timer()

    override fun afterTextChanged(s: Editable) {
        timer.cancel()
        timer = Timer()
        timer.schedule(
            object : TimerTask() {
                override fun run() {
                    keyChanged.onNext(s.toString())
                }
            },
            300
        )
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
}