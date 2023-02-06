package ru.beru.sortingcenter.mocks

import android.content.SharedPreferences.Editor

internal class EditorImpl : Editor {
    private val newValuesMap: MutableMap<String, Any?> = HashMap()
    private var shouldClear = false
    override fun putString(key: String, value: String?): Editor {
        newValuesMap[key] = value
        return this
    }

    override fun putStringSet(key: String, values: Set<String?>?): Editor {
        newValuesMap[key] = values?.let { HashSet(it) }
        return this
    }

    override fun putInt(key: String, value: Int): Editor {
        newValuesMap[key] = value
        return this
    }

    override fun putLong(key: String, value: Long): Editor {
        newValuesMap[key] = value
        return this
    }

    override fun putFloat(key: String, value: Float): Editor {
        newValuesMap[key] = value
        return this
    }

    override fun putBoolean(key: String, value: Boolean): Editor {
        newValuesMap[key] = value
        return this
    }

    override fun remove(key: String): Editor {
        // 'this' is marker for remove operation
        newValuesMap[key] = this
        return this
    }

    override fun clear(): Editor {
        shouldClear = true
        return this
    }

    override fun commit(): Boolean {
        apply()
        return true
    }

    override fun apply() {
    }
}