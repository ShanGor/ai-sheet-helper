package io.github.shangor.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class MergedRegion {
    private int startRow;
    private int endRow;
    private int startColumn;
    private int endColumn;
} 