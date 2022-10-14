package me.yuugiri.fluiditygradle.utils

class MergedMap<K, out V>(val maps: List<Map<K, V>>) {

    operator fun get(key: K): V? {
        maps.forEach {
            val value = it[key]
            if (value != null) return value
        }
        return null
    }
}