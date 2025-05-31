package io.github.shangor.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class SheetData {
    private String sheetName;
    private int sheetIndex;
    private List<CellData> cells;
    private List<MergedRegion> mergedRegions;
} 