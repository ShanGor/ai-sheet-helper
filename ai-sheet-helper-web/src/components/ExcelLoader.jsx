import React, { useState } from 'react';

const ExcelLoader = ({ univerAPI }) => {
  const [isLoading, setIsLoading] = useState(false);

  const handleFileChange = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    setIsLoading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch('http://localhost:8080/api/excel/upload', {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        console.log("Error message: ", await response.text());
        throw new Error('Failed to upload file');
      }

      const sheets = await response.json();
      
      // Check if there's already an active workbook and use it
      let workbook = univerAPI.getActiveWorkbook();
      
      // If no workbook exists, create a new one
      if (!workbook) {
        workbook = univerAPI.createWorkbook({ 
          name: file.name.replace(/\.[^/.]+$/, "")
        });
      }
      
      // Create sheets for each Excel sheet
      sheets.forEach((sheetData, index) => {
        let worksheet;
        
        try {
          // Create sheet with the correct name from the start
          worksheet = workbook.create(
            sheetData.sheetName || `Sheet${index + 1}`, 
            1000, // rows
            100   // columns
          );
        } catch (e) {
          console.warn(`Failed to create sheet ${sheetData.sheetName}:`, e);
          return;
        }
        
        if (!worksheet) return;
        
        // Set cell values and formulas
        sheetData.cells.forEach(cell => {
          try {
            const range = worksheet.getRange(cell.row, cell.column);
            if (range) {
              // If cell has a formula, set the formula; otherwise set the value
              if (cell.formula && cell.formula.startsWith('=')) {
                try {
                  range.setFormula(cell.formula);
                } catch (formulaError) {
                  console.warn(`Failed to set formula ${cell.formula} for cell ${cell.row},${cell.column}, setting value instead:`, formulaError);
                  range.setValue(cell.value);
                }
              } else if (cell.value !== null && cell.value !== undefined && cell.value !== '') {
                range.setValue(cell.value);
              }

              // Apply styles if available
              if (cell.style && cell.style !== '' && cell.style !== '{}') {
                applyStyle(range, cell.style, cell, worksheet);
              }
            }
          } catch (e) {
            console.warn(`Failed to set value for cell ${cell.row},${cell.column}:`, e);
          }
        });
        
        // Apply merged cells
        if (sheetData.mergedRegions && sheetData.mergedRegions.length > 0) {
          sheetData.mergedRegions.forEach(mergedRegion => {
            try {
              // Get the range for the merged region
              const startRow = mergedRegion.startRow;
              const startCol = mergedRegion.startColumn;
              const rowCount = mergedRegion.endRow - mergedRegion.startRow + 1;
              const colCount = mergedRegion.endColumn - mergedRegion.startColumn + 1;
              
              const range = worksheet.getRange(startRow, startCol, rowCount, colCount);
              if (range && range.merge) {
                range.merge();
              } else {
                // Try using command if direct method not available
                worksheet.setActiveRange(range);
                univerAPI.executeCommand('sheet.command.merge-cell');
              }
            } catch (e) {
              console.warn(`Failed to merge cells ${mergedRegion.startRow},${mergedRegion.startColumn} to ${mergedRegion.endRow},${mergedRegion.endColumn}:`, e);
            }
          });
        }
      });
      
      // Switch to the first sheet
      if (sheets.length > 0) {
        const firstSheet = workbook.getSheetByName(sheets[0].sheetName);
        if (firstSheet) {
          workbook.setActiveSheet(firstSheet);
        } else {
          // If getSheetByName doesn't work, try getting by index
          const firstWorksheet = workbook.getSheetByIndex(0);
          if (firstWorksheet) {
            workbook.setActiveSheet(firstWorksheet);
          }
        }
      }

      console.log(`Excel file loaded successfully with ${sheets.length} sheet(s)`);
    } catch (error) {
      console.error('Error loading Excel file:', error);
      alert(error.message || 'Error loading Excel file. Please try again.');
    } finally {
      setIsLoading(false);
      // Reset the file input
      event.target.value = '';
    }
  };

  const applyStyle = (range, styleString, cell, worksheet) => {
    try {
      const styles = JSON.parse(styleString);
      
      // Apply background color - this method works
      if (styles.backgroundColor && range.setBackgroundColor) {
        try {
          range.setBackgroundColor(styles.backgroundColor);
        } catch (e) {
          console.warn(`Failed to set background color for cell ${cell.row},${cell.column}:`, e);
        }
      }

      // Log all styles that would be applied but skip commands that don't work
      const styleInfo = {};
      if (styles.bold) styleInfo.bold = styles.bold;
      if (styles.italic) styleInfo.italic = styles.italic;
      if (styles.underline) styleInfo.underline = styles.underline;
      if (styles.fontSize) styleInfo.fontSize = styles.fontSize;
      if (styles.fontColor) styleInfo.fontColor = styles.fontColor;
      if (styles.alignment) styleInfo.alignment = styles.alignment;
      
      if (Object.keys(styleInfo).length > 0) {
        // console.log(`Cell ${cell.row},${cell.column} has styles:`, styleInfo);
        // Note: Font and alignment styling commands are not available in the current Univer.js setup
        // The styling information is preserved in the console for reference
      }
    } catch (styleError) {
      console.warn('Failed to parse or apply style:', styleError);
    }
  };

  return (
    <div style={{ marginBottom: '20px' }}>
      <input
        type="file"
        accept=".xlsx,.xls"
        onChange={handleFileChange}
        style={{ display: 'none' }}
        id="excel-file-input"
        disabled={isLoading}
      />
      <button
        onClick={() => document.getElementById('excel-file-input').click()}
        style={{
          padding: '8px 16px',
          backgroundColor: isLoading ? '#cccccc' : '#4CAF50',
          color: 'white',
          border: 'none',
          borderRadius: '4px',
          cursor: isLoading ? 'not-allowed' : 'pointer',
          fontSize: '14px',
          width: '100%'
        }}
        disabled={isLoading}
      >
        {isLoading ? 'Loading...' : 'Load Excel File'}
      </button>
    </div>
  );
};

export default ExcelLoader;