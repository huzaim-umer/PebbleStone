package com.example.pebblestone.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.pebblestone.R

/**
 * Thin wrapper over [SoundPool] for the game's short effects. Sounds are tiny
 * generated WAVs in res/raw and load asynchronously; a play() before load simply
 * no-ops, which is fine for such brief clips.
 */
class SoundManager(context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val move = pool.load(context, R.raw.move, 1)
    private val remove = pool.load(context, R.raw.remove, 1)
    private val win = pool.load(context, R.raw.win, 1)
    private val lose = pool.load(context, R.raw.lose, 1)

    var muted: Boolean = false

    private fun play(id: Int, volume: Float = 1f) {
        if (muted) return
        pool.play(id, volume, volume, 1, 0, 1f)
    }

    /** A committed jump: wooden tock plus a soft capture pop. */
    fun playMove() {
        play(move, 0.9f)
        play(remove, 0.5f)
    }

    fun playWin() = play(win, 1f)

    fun playLose() = play(lose, 0.9f)

    fun release() = pool.release()
}
