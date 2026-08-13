package com.mrtnmrls.music_tracker_app.ui.stats

import android.icu.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

data class SelectedMonth(
    val year: Int,
    val month: Int
) {

    fun previous() = if (month == 1) SelectedMonth(year - 1, 12) else SelectedMonth(year, month - 1)
    fun next() = if (month == 12) SelectedMonth(year + 1, 1) else SelectedMonth(year, month + 1)

    fun displayName(): String {
        val cal = Calendar.getInstance().apply { set(year, month - 1, 1) }
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    fun toEpochRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return start to cal.timeInMillis
    }

    companion object {
        fun current(): SelectedMonth {
            val cal = Calendar.getInstance()
            return SelectedMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }
    }
}