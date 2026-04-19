package util;

import java.util.*;

public class AuditLogger {
    private final List<String> entries = new ArrayList<>();

    public void log(String entry) {
        entries.add(String.format("[%s] %s", new Date(), entry));
    }

    public List<String> getAllEntries() {
        return Collections.unmodifiableList(entries);
    }
}
