package com.emove.emoveapplication.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by lipponep on 23.11.2017.
 */

public class MdsLogbookEntriesResponse {

    public MdsLogbookEntriesResponse(LogEntry[] logEntries) {
        this.logEntries = logEntries;
    }

    @SerializedName("elements")
    public final LogEntry[] logEntries;

    public static class LogEntry {
        @SerializedName("Id")
        public final int id;

        @SerializedName("ModificationTimestamp")
        public final long modificationTimestamp;

        @SerializedName("Size")
        public final Long size;

        LogEntry(int id, int modificationTimestamp, Long size) {
            this.id = id;
            this.modificationTimestamp = modificationTimestamp;
            this.size = size;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append(id);
            sb.append(" : ");
            sb.append(modificationTimestamp);
            sb.append(" : ");
            sb.append(size);

            return sb.toString();
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (LogEntry le : logEntries) {
            if (sb.length() > 0)
                sb.append("\n");

            sb.append(le.id);
            sb.append(" : ");
            sb.append(le.modificationTimestamp);
            sb.append(" : ");
            sb.append(le.size);
        }
        return sb.toString();
    }

}
