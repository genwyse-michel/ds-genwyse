package com.genwyse.poi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Report  {
  public static enum Border {
    Left,
    Right,
    Top,
    Bottom,
    MiddleVertical,
    MiddleHorizontal
  }
  
  public static enum BorderStyle {
    None (CellStyle.BORDER_NONE),
    Hair (CellStyle.BORDER_HAIR),
    Thin (CellStyle.BORDER_THIN),
    Thick (CellStyle.BORDER_THICK),
    Medium (CellStyle.BORDER_MEDIUM),
    Double (CellStyle.BORDER_DOUBLE),
    ;
    
    private final short value;
    private BorderStyle(short value) 
    {
      this.value = value;
    }
  }
  
  public static class TableRow {
    Map<String,Object> values = new HashMap<String,Object> ();
    Map<String,String> urls = new HashMap<String,String> ();
    
    public TableRow ()
    {
      
    }
    
    public void put (String name, Object value)
    {
      values.put (name, value);
    }
    
    public void putUrl (String name, String url)
    {
      urls.put (name, url);
    }
  }
  
  Map<String,Integer> dataLineCounts = new HashMap<String,Integer> ();
  Workbook workbook;
  File modelFile;
  String name;
  String ext;
  Map<String,CellStyle> namedStyles = new HashMap<String,CellStyle> ();
  
  public Report (String model_path) throws IOException
  {
    modelFile = new File (model_path);
    String file_name = modelFile.getName();
    int pos_dot = file_name.lastIndexOf('.');
    if (pos_dot==-1) {
      ext = "";
      name = file_name;
    }
    else {
      ext = file_name.substring(pos_dot);
      name = file_name.substring(0, pos_dot);
    }
    
    FileInputStream input_stream = new FileInputStream(modelFile);
    if(".xlsx".equals(ext)) {
      workbook = new XSSFWorkbook(input_stream);
    }
    else {
      HSSFWorkbook hwb = new HSSFWorkbook(input_stream);
      short num_styles = hwb.getNumCellStyles();
      for (short i=0;i<num_styles;i++) {
        HSSFCellStyle style = hwb.getCellStyleAt(i);
        String s = style.getUserStyleName();
        if (s!=null) {
          namedStyles.put(s, style);
        }
      }

      workbook = hwb;
    }
    input_stream.close();
  }

  public String getReportName ()
  {
    return name;
  }
  
  public String getReportExt ()
  {
    return ext;
  }
  
  protected Cell getCell (Sheet sheet, int nColumn, int nRow, boolean create)
  {
    // Sheet sheet = workbook.getSheetAt(workbook.getActiveSheetIndex());
    Row row = sheet.getRow(nRow);
    if (row==null) {
      if (!create) return null;
      else {
        row = sheet.createRow(nRow);
      }
    }
    Cell cell = row.getCell(nColumn);
    if (cell==null) {
      if (!create) return null;
      else {
        cell = row.createCell(nColumn);
      }
    }
    return cell;
  }
  
  protected Cell getNamedCell (String cell_name)
  {
    Name named_region = workbook.getName(cell_name);
    if (named_region!=null) {
      AreaReference area_ref = new AreaReference(named_region.getRefersToFormula());
      CellReference cell_ref = area_ref.getFirstCell();
      Sheet sheet = workbook.getSheet(cell_ref.getSheetName());
      Row row = sheet.getRow(cell_ref.getRow());
      Cell cell = row.getCell(cell_ref.getCol());
      return cell;
    }
    return null;
  }
  
  protected CellRangeAddress getNamedCellRange (String cell_name)
  {
    Name named_region = workbook.getName(cell_name);
    if (named_region!=null) {
      AreaReference area_ref = new AreaReference(named_region.getRefersToFormula());
      CellReference first_cell_ref = area_ref.getFirstCell();
      CellReference last_cell_ref = area_ref.getLastCell();
      return new CellRangeAddress(first_cell_ref.getRow(), last_cell_ref.getRow(), first_cell_ref.getCol(), last_cell_ref.getCol());
    }
    return null;
  }
  
  /*
   * InsËre un bloc de lignes.
   * Les styles du bloc original sont copiÈs sur demande.
   */
  protected void insertLines (Sheet sheet, int at_row, int from_row, int row_count, boolean copy_styles)
  {
    // DÈcalage vers le bas
    sheet.shiftRows(at_row, sheet.getLastRowNum(), row_count);
    for (int num_line = 0; num_line<row_count; num_line++) {
      Row inserted_row = sheet.getRow(at_row+num_line);
      if (inserted_row == null) {
        sheet.createRow(at_row+num_line); 
      }
    }
    
    if (copy_styles) { 
      // Copie des styles des lignes d'origine
      for (int num_line = 0; num_line<row_count; num_line++) {
        Row inserted_row = sheet.getRow(at_row+num_line);
        Row original_row = sheet.getRow(from_row+num_line);
        //inserted_row.setHeight((short)255);
        Iterator<Cell> i_cell = original_row.cellIterator();
        while (i_cell.hasNext()) {
          Cell original_cell = i_cell.next();
          int num_cell = original_cell.getColumnIndex();
          Cell inserted_cell = inserted_row.getCell(num_cell, Row.CREATE_NULL_AS_BLANK); 
          
          inserted_cell.setCellStyle(original_cell.getCellStyle());
          inserted_cell.setCellType(original_cell.getCellType());
        }
      }
    }
     
  }
  
  public int getTableHeight (String table_name)
  {
    int num_line = 0;
    if (dataLineCounts.containsKey(table_name)) {
      num_line = dataLineCounts.get(table_name);
    }
    return num_line;
  }
  
  private void setTableHeight (String table_name, int num_line)
  {
    dataLineCounts.put(table_name, num_line);
  }
  
  public void addData (String data_name, Object data, int num_data)
  {
    TableRow data_line = new TableRow();
    data_line.put(data_name, data);
    addTableRow (data_name, data_line);
  }
  
  /*
   * Ajoute une ligne dans une table de valeurs
   * 
   * Retourne le nombre de lignes de la table
   */
  public int addTableRow (String data_name, TableRow data)
  {
    Name named_region = workbook.getName(data_name);

    if (named_region!=null) {
      // Emplacement de la table: feuille, cellule dÈbut, cellule fin
      AreaReference area_ref = new AreaReference(named_region.getRefersToFormula());
      CellReference first_cell_ref = area_ref.getFirstCell();
      CellReference last_cell_ref = area_ref.getLastCell();
      Sheet sheet = workbook.getSheet(first_cell_ref.getSheetName());
      
      // NumÈro de ligne de la table
      int num_line = getTableHeight (data_name);
      
      int data_row = first_cell_ref.getRow();
      int line_count = last_cell_ref.getRow()-data_row+1;
      int add_data_row = data_row+num_line*line_count;
      if (num_line>0) {
        insertLines (sheet, add_data_row, data_row, line_count, true);
      }
      setTableHeight (data_name, num_line+1);
      
      Set<Entry<String, Object>> entries = data.values.entrySet();
      Iterator<Entry<String, Object>> i_data = entries.iterator();
      while (i_data.hasNext()) {
        Entry<String, Object> e = i_data.next();
        String name = e.getKey();
        Object value = e.getValue();
        
        Cell first_data_cell = getNamedCell(name);
        if (first_data_cell!=null) {
          int num_col = first_data_cell.getColumnIndex();
          int num_row = data_row+num_line*line_count;
          if (line_count>1) {
            // Zone de donnÈes sur plusieurs lignes, il faut dÈterminer la position relative de la cellule
            int relative_row = first_data_cell.getRowIndex() - data_row;
            num_row += relative_row;
          }
          Row row = sheet.getRow(num_row);
          Cell cell = row.getCell(num_col, Row.CREATE_NULL_AS_BLANK);
          setValue (cell, value);
          
          if (data.urls!=null && data.urls.containsKey(name)) {
            setURL (cell, data.urls.get(name));
          }
        }
      }
      return num_line+1;
    }
    else {
      return -1;
    }
  }
 
  public void setURL (String value_name, int column, int row, String value)
  {
    Cell cell = getNamedCell (value_name);
    
    setURL (cell.getSheet(), cell.getColumnIndex()+column, cell.getRowIndex()+row, value);
  }
  
  protected void setURL (Sheet sheet, int column, int row, String value)
  {
    Cell cell = getCell (sheet, column, row, true);
    setURL(cell, value);
  }
  
  public void setURL (int column, int row, String value)
  {
    setURL(workbook.getSheetAt(workbook.getActiveSheetIndex()), column, row, value);
  }
  
  public void setURL (Cell cell, String value)
  {
    if (value!=null && !"".equals(value)) {
      Hyperlink link = workbook.getCreationHelper().createHyperlink(Hyperlink.LINK_URL);
      link.setAddress(value.toString());
      link.setLabel(cell.getStringCellValue());
      int col = cell.getColumnIndex();
      int row = cell.getRowIndex();
      link.setFirstColumn(col);
      link.setLastColumn(col);
      link.setFirstRow(row);
      link.setLastRow(row);
      cell.setHyperlink(link);
    }
  }
  
  /*
   * Valeur pour la cellule courante d'une table nomm√©e
   */
  public void setValue (String value_name, Object value)
  {
    Cell cell = getNamedCell (value_name);
    setValue (cell, value);
  }
  
  public void setValue (int column, int row, Object value)
  {
    setValue(workbook.getSheetAt(workbook.getActiveSheetIndex()), column, row, value);
  }
  
  protected void setValue (Sheet sheet, int column, int row, Object value)
  {
    Cell cell = getCell (sheet, column, row, true);
    setValue (cell, value);
  }
  
  /*
   * Valeur pour une cellule dans une table nomm√©e
   */
  public void setValue (String value_name, int column, int row, Object value)
  {
    Cell cell = getNamedCell (value_name);
    if (cell!=null) {
      setValue (cell.getSheet(), cell.getColumnIndex()+column, cell.getRowIndex()+row, value);
    }
  }
  
  public Object getValue(String value_name, int column, int row) {
    Cell cell = getNamedCell (value_name);
    if (cell==null) return null; // La donn√©e n'existe pas
    
    Sheet sheet = cell.getSheet();
    cell = getCell (sheet, cell.getColumnIndex()+column, cell.getRowIndex()+row, false);
    if (cell==null) return null; // La cellule en (column, row) n'existe pas
    switch (cell.getCellType()) {
    case Cell.CELL_TYPE_STRING:
      return cell.getStringCellValue();
    case Cell.CELL_TYPE_BOOLEAN:
      return new Boolean(cell.getBooleanCellValue());
    case Cell.CELL_TYPE_NUMERIC:
      return new Double(cell.getNumericCellValue());
    case Cell.CELL_TYPE_BLANK:
      return new String();
    case Cell.CELL_TYPE_FORMULA:
      // Il faut prendre la valeur cach√©e
      switch (cell.getCachedFormulaResultType()) {
      case Cell.CELL_TYPE_STRING:
        return cell.getStringCellValue();
      case Cell.CELL_TYPE_BOOLEAN:
        return new Boolean(cell.getBooleanCellValue());
      case Cell.CELL_TYPE_NUMERIC:
        return new Double(cell.getNumericCellValue());
      default:
        return null; // Valeur inconnue
      }
    default:
      return null; // Valeur inconnue
    }
  }
  
  protected void setValue (Cell cell, Object value)
  {
    if (cell!=null) {
      if (value == null) {
        cell.setCellValue("");
      }
      else if (value instanceof String) {
        cell.setCellValue((String) value);
      }
      else if (value instanceof Boolean) {
        cell.setCellValue((Boolean) value);
      }
      else if (value instanceof Calendar) {
        cell.setCellValue((Calendar) value);
      }
      else if (value instanceof Date) {
        cell.setCellValue((Date) value);
      }
      else if (value instanceof Double) {
        cell.setCellValue((Double) value);
      }
      else if (value instanceof RichTextString) {
        cell.setCellValue((RichTextString) value);
      }
      else if (value instanceof Integer) {
        cell.setCellValue((Integer) value);
      }
      else {
        cell.setCellValue(value.toString());
      }
    }
  }
  
  public void setStyle (String cell_name, String style_name)
  {
    Cell cell = getNamedCell (cell_name);
    if (cell!=null) {
      setStyle (cell, style_name);
    }
  }
  
  protected void setStyle (Cell cell, String style_name)
  {
    CellStyle style = namedStyles.get(style_name);
    if (style!=null) {
      cell.setCellStyle(style);
    }
  }
  
  public void save (File report_file) throws IOException
  {
    OutputStream wb_os = new FileOutputStream (report_file);
    workbook.write(wb_os);
    wb_os.close();
  }
  
  public void save (OutputStream wb_os) throws IOException
  {
    workbook.write(wb_os);
  }
  
  public void mergeArea (String data_name, int first_row, int last_row, int first_col, int last_col)
  {
    Name named_region = workbook.getName(data_name);

    if (named_region!=null) {
      AreaReference area_ref = new AreaReference(named_region.getRefersToFormula());
      CellReference first_cell_ref = area_ref.getFirstCell();
      Sheet sheet = workbook.getSheet(first_cell_ref.getSheetName());
      CellRangeAddress cell_range = new CellRangeAddress(
          first_cell_ref.getRow()+first_row, 
          first_cell_ref.getRow()+last_row, 
          first_cell_ref.getCol()+first_col, 
          first_cell_ref.getCol()+last_col);
      sheet.addMergedRegion(cell_range);
    }
  }
  public void setBorder (String table_name, Border border, BorderStyle style)
  {
    Name named_region = workbook.getName(table_name);
    if (named_region!=null) {
      AreaReference area_ref = new AreaReference(named_region.getRefersToFormula());
      CellReference first_cell_ref = area_ref.getFirstCell();
      CellReference last_cell_ref = area_ref.getLastCell();
      Sheet sheet = workbook.getSheet(first_cell_ref.getSheetName());
      int table_height = getTableHeight (table_name);
      int first_row = first_cell_ref.getRow();
      int first_col = first_cell_ref.getCol();
      int last_row = last_cell_ref.getRow() + table_height - 1;
      int last_col = last_cell_ref.getCol();
  
      switch (border) {
      case Top: {
        Row row = sheet.getRow(first_row);
        for (int num_col = first_col; num_col <= last_col; num_col++) {
          Cell cell = row.getCell(num_col, Row.CREATE_NULL_AS_BLANK);
          CellUtil.setCellStyleProperty(cell, workbook, CellUtil.BORDER_TOP, style.value);
        }
        break;
      }
      case Bottom: {
        Row row = sheet.getRow(last_row);
        for (int num_col = first_col; num_col <= last_col; num_col++) {
          Cell cell = row.getCell(num_col, Row.CREATE_NULL_AS_BLANK);
          CellUtil.setCellStyleProperty(cell, workbook, CellUtil.BORDER_BOTTOM, style.value);
        }
        break;
      }
      case Left:
        for (int num_row = first_row; num_row <= last_row; num_row++) {
          Row row = sheet.getRow(num_row);
          Cell cell = row.getCell(first_col, Row.CREATE_NULL_AS_BLANK);
          CellUtil.setCellStyleProperty(cell, workbook, CellUtil.BORDER_LEFT, style.value);
        }
        break;
      case Right:
        for (int num_row = first_row; num_row <= last_row; num_row++) {
          Row row = sheet.getRow(num_row);
          Cell cell = row.getCell(last_col, Row.CREATE_NULL_AS_BLANK);
          CellUtil.setCellStyleProperty(cell, workbook, CellUtil.BORDER_RIGHT, style.value);
        }
        break;
      }
    }
  }
  
  /*
   * Fonction de retaillage automatique: ATTENTION, cette fonction utilise les propri√©t√©s de l'affichage graphique de l'application
   * Java, il faut donc soit avoir un √©cran X11 disponible (si serveur Linux), soit avoir d√©sactiv√© l'affichage effectif avec
   * l'option -Djava.awt.headless=true au d√©marrage de Tomcat
   */
  protected void autoSizeColumn(Sheet sheet, int numColumn) {
    if (sheet!=null) {
      sheet.autoSizeColumn(numColumn, true);
    }
  }
  
  public void autoSizeColumn(int numSheet, int numColumn) {
    autoSizeColumn (workbook.getSheetAt(numSheet), numColumn);
  }
  
  public void autoSizeColumn(int numColumn) {
    autoSizeColumn(workbook.getActiveSheetIndex(), numColumn);
  }
  
  public void autoSizeColumns(int numSheet, int firstcolumn, int lastColumn) {
    for (int i=firstcolumn; i<=lastColumn; i++) {
      autoSizeColumn(numSheet, i);
    }
  }
  
  public void autoSizeColumns(int firstcolumn, int lastColumn) {
    autoSizeColumns(workbook.getActiveSheetIndex(), firstcolumn, lastColumn);
  }
}
