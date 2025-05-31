package io.github.shangor.controller;

import io.github.shangor.model.SheetData;
import io.github.shangor.service.ExcelService;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/excel")
public class ExcelController {
    
    private final ExcelService excelService;
    
    public ExcelController(ExcelService excelService) {
        this.excelService = excelService;
    }
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<List<SheetData>> uploadExcel(@RequestPart("file") FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
            .map(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                return bytes;
            })
            .flatMap(fileContent -> excelService.processExcelFile(fileContent).next());
    }
} 