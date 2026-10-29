package dev.mfgdiff;

import java.util.List;

/** Result of comparing two revisions. Indices are positions in the revision's ordered segment/item lists. */
public record DiffResult(Definition a, Definition b, List<SegmentRow> segments) {

    public enum Status { SAME, CHANGED, ADDED, REMOVED }

    /** oldValue / newValue are null when the field is absent on that side. */
    public record FieldChange(String field, String oldValue, String newValue) {
    }

    /** a = item index in the A bill, b = item index in the B bill; -1 when the item is absent there. */
    public record ItemRow(Status status, int a, int b, List<FieldChange> changes) {
    }

    /**
     * a / b are positions of the segment in revision A / B (-1 when absent). {@code moved} can be set
     * together with SAME or CHANGED. Items of an ADDED / REMOVED segment are ADDED / REMOVED.
     */
    public record SegmentRow(Status status, boolean moved, int a, int b, List<FieldChange> changes,
            List<ItemRow> items) {
    }
}
