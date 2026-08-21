package com.pit.prizrak.background

/**
 * Ring buffer of recently captured frames, used as the source of "what was behind the
 * person a moment ago" pixels. Oldest frames are evicted (and their Mats released) once
 * capacity is exceeded.
 */
class FrameHistory(private val capacity: Int = 12) {

    private val buffer = ArrayDeque<FrameRecord>()

    @Synchronized
    fun push(record: FrameRecord) {
        buffer.addLast(record)
        while (buffer.size > capacity) {
            buffer.removeFirst().release()
        }
    }

    /** Newest frame first - the most likely to still resemble the current view. */
    @Synchronized
    fun snapshotNewestFirst(limit: Int = capacity): List<FrameRecord> =
        buffer.asReversed().take(limit)

    @Synchronized
    fun clear() {
        buffer.forEach { it.release() }
        buffer.clear()
    }

    @Synchronized
    fun size(): Int = buffer.size
}
