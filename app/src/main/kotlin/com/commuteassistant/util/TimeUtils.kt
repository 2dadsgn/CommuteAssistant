package com.commuteassistant.util

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

object TimeUtils {
    /**
     * Calculates the next occurrence of a specific [dayOfWeek] and [time].
     * If the time has already passed today, it moves to the next week.
     */
    fun calculateNextOccurrence(dayOfWeek: DayOfWeek, time: LocalTime): LocalDateTime {
        val now = LocalDateTime.now()
        var targetDate = now.toLocalDate()
        
        // Advance to the next day matching dayOfWeek
        while (targetDate.dayOfWeek != dayOfWeek) {
            targetDate = targetDate.plusDays(1)
        }
        
        var targetDateTime = LocalDateTime.of(targetDate, time)
        
        // If the calculated time has already passed today, move to next week
        if (targetDateTime.isBefore(now)) {
            targetDateTime = targetDateTime.plusWeeks(1)
        }
        
        return targetDateTime
    }
}
