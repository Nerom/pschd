package wang.nerom.pschd.util

import kotlin.random.Random

object RandomUtil {
    private val random = Random(System.currentTimeMillis())
    /**
     * @param until exclusive
     */
    fun random(until: Long): Long {
        return random.nextLong(until)
    }
}