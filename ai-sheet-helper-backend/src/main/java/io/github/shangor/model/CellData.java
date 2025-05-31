package io.github.shangor.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class CellData {
    private String value;
    private String type;
    private String formula;
    private String style;
    private int row;
    private int column;
} 