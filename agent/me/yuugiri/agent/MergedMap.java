package me.yuugiri.agent;

import java.util.List;
import java.util.Map;

public class MergedMap {

    public List<Map<String, String>> maps;

    public MergedMap(List<Map<String, String>> maps) {
        this.maps = maps;
    }

    public String get(String key) {
        for (Map<String, String> map : maps) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    public String getWithFallback(String key, String fallback) {
        for (Map<String, String> map : maps) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return fallback;
    }
}
