package dev.acciv.apicanon;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/** Structural diff between two canonical requests. */
public final class Diff {
    private static final String MISSING = "<missing>";

    private Diff() {
    }

    public static List<Object> between(Map<String, Object> left, Map<String, Object> right) {
        List<Object> changes = new ArrayList<>();
        walk(left, right, "$", changes);
        return changes;
    }

    private static void walk(Object left, Object right, String path, List<Object> changes) {
        if (left instanceof Map<?, ?> leftMap && right instanceof Map<?, ?> rightMap) {
            TreeSet<String> keys = new TreeSet<>();
            for (Object key : leftMap.keySet()) {
                keys.add(String.valueOf(key));
            }
            for (Object key : rightMap.keySet()) {
                keys.add(String.valueOf(key));
            }
            for (String key : keys) {
                Object a = leftMap.containsKey(key) ? leftMap.get(key) : null;
                Object b = rightMap.containsKey(key) ? rightMap.get(key) : null;
                String childPath = path.equals("$") ? "$." + key : path + "." + key;
                if (!leftMap.containsKey(key)) {
                    add(childPath, "added", MISSING, b, changes);
                } else if (!rightMap.containsKey(key)) {
                    add(childPath, "removed", a, MISSING, changes);
                } else {
                    walk(a, b, childPath, changes);
                }
            }
            return;
        }

        if (left instanceof List<?> leftList && right instanceof List<?> rightList) {
            int shared = Math.min(leftList.size(), rightList.size());
            for (int i = 0; i < shared; i++) {
                walk(leftList.get(i), rightList.get(i), path + "[" + i + "]", changes);
            }
            if (leftList.size() > rightList.size()) {
                add(path + ".length", "changed", leftList.size(), rightList.size(), changes);
            } else if (leftList.size() < rightList.size()) {
                add(path + ".length", "changed", leftList.size(), rightList.size(), changes);
            }
            return;
        }

        if (!Objects.equals(left, right)) {
            add(path, "changed", left, right, changes);
        }
    }

    private static void add(String path, String type, Object before, Object after, List<Object> changes) {
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("path", path);
        change.put("type", type);
        change.put("before", before);
        change.put("after", after);
        changes.add(change);
    }
}
