package org.hl7.fhir.igtools.spreadsheets;

import org.hl7.fhir.utilities.xhtml.XhtmlNode;

public class MappingSpace {

  private String columnName; // in the spreadsheets
  private String title; // in the specification
  private XhtmlNode preamble; // html to go in spec
  private String id; // internal page reference
  private int sortOrder; 
  private boolean publish;
  
  public MappingSpace(String columnName, String title, String id, int sortOrder, boolean publish) {
    super();
    this.columnName = columnName;
    this.title = title;
    this.id = id;
    this.sortOrder = sortOrder;
    this.preamble = null;
    this.publish = publish;
  }

  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public XhtmlNode getPreamble() {
    return preamble;
  }

  public void setPreamble(XhtmlNode preamble) {
    this.preamble = preamble;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  public boolean isPublish() {
    return publish;
  }

  public void setPublish(boolean publish) {
    this.publish = publish;
  } 
  
}
