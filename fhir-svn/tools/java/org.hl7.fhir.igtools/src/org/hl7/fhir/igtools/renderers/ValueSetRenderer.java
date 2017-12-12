package org.hl7.fhir.igtools.renderers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r4.terminologies.ValueSetUtilities;
import org.hl7.fhir.r4.utils.EOperationOutcome;
import org.hl7.fhir.r4.utils.NarrativeGenerator;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.igtools.publisher.FetchedResource;
import org.hl7.fhir.igtools.publisher.IGKnowledgeProvider;
import org.hl7.fhir.igtools.publisher.SpecMapManager;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.xhtml.XhtmlComposer;

public class ValueSetRenderer extends BaseRenderer {

  private ValueSet vs;

  public ValueSetRenderer(IWorkerContext context, String prefix, ValueSet vs, IGKnowledgeProvider igp, List<SpecMapManager> maps) {
    super(context, prefix, igp, maps);
    this.vs = vs;
  }

  public String summary(FetchedResource r, boolean xml, boolean json, boolean ttl) throws Exception {
    StringBuilder b = new StringBuilder();
    b.append("<table class=\"grid\">\r\n");
    b.append(" <tbody><tr><td>"+translate("vs.summary", "Defining URL")+":</td><td>"+Utilities.escapeXml(vs.getUrl())+"</td></tr>\r\n");
    b.append(" <tr><td>"+translate("vs.summary", "Name")+":</td><td>"+Utilities.escapeXml(gt(vs.getNameElement()))+"</td></tr>\r\n");
    b.append(" <tr><td>"+translate("vs.summary", "Definition")+":</td><td>"+processMarkdown("description", vs.getDescriptionElement())+"</td></tr>\r\n");
    if (vs.hasPublisher())
      b.append(" <tr><td>"+translate("vs.summary", "Publisher")+":</td><td>"+Utilities.escapeXml(gt(vs.getPublisherElement()))+"</td></tr>\r\n");
    if (ValueSetUtilities.hasOID(vs))
      b.append(" <tr><td>"+translate("vs.summary", "OID")+":</td><td>"+ValueSetUtilities.getOID(vs)+"("+translate("vs.summary", "for OID based terminology systems")+")</td></tr>\r\n");
    if (vs.hasCopyright())
      b.append(" <tr><td>"+translate("vs.summary", "Copyright")+":</td><td>"+Utilities.escapeXml(gt(vs.getCopyrightElement()))+"</td></tr>\r\n");
    if (xml || json || ttl) {
      b.append(" <tr><td>"+translate("vs.summary", "Source Resource")+"</td><td>");
      boolean first = true;
      String filename = igp.getProperty(r, "format");
      if (filename == null)
        filename = "ValueSet-"+r.getId()+".{{[fmt]}}.html";
      if (xml) {
        first = false;
        b.append("<a href=\""+igp.doReplacements(filename,  r,  null, "xml")+"\">"+translate("vs.summary", "XML")+"</a>");
      }
      if (json) {
        if (first) first = false; else b.append(" / ");
        b.append("<a href=\""+igp.doReplacements(filename,  r,  null, "json")+"\">"+translate("vs.summary", "JSON")+"</a>");
      }
      if (ttl) {
        if (first) first = false; else b.append(" / ");
        b.append("<a href=\""+igp.doReplacements(filename,  r,  null, "ttl")+"\">"+translate("vs.summary", "Turtle")+"</a>");
      }
      b.append("</td></tr>\r\n");
    }
    b.append("</tbody></table>\r\n");

    return b.toString();
  }

  public String cld() throws EOperationOutcome, FHIRException, IOException, org.hl7.fhir.exceptions.FHIRException  {
    if (vs.hasText() && vs.getText().hasDiv())
      return new XhtmlComposer().compose(vs.getText().getDiv());
    ValueSet vsc = vs.copy();
    vsc.setText(null);
    new NarrativeGenerator("", "", context).generate(vsc);

    return new XhtmlComposer().compose(vsc.getText().getDiv());
  }

  public String xref() throws FHIRException {
    StringBuilder b = new StringBuilder();
    boolean first = true;
    b.append("\r\n");
    List<String> sdurls = new ArrayList<String>();
    List<String> vsurls = new ArrayList<String>();
    for (MetadataResource sd : context.allConformanceResources()) {
      if (sd instanceof StructureDefinition)
        sdurls.add(sd.getUrl());
      if (sd instanceof ValueSet)
        vsurls.add(sd.getUrl());
    }
    Collections.sort(sdurls);
    Collections.sort(vsurls);
    
    for (String url : vsurls) {
      ValueSet vc = context.fetchResource(ValueSet.class, url);
      for (ConceptSetComponent t : vc.getCompose().getInclude()) {
        for (UriType ed : t.getValueSet()) {
          if (ed.getValueAsString().equals(vs.getUrl())) {
            if (first) {
              first = false;
              b.append("<ul>\r\n");
            }
            b.append(" <li>"+translate("vs.usage", "Included into ")+"<a href=\""+vc.getUserString("path")+"\">"+Utilities.escapeXml(gt(vc.getNameElement()))+"</a></li>\r\n");
            break;
          }
        }
      }
      for (ConceptSetComponent t : vc.getCompose().getExclude()) {
        for (UriType ed : t.getValueSet()) {
          if (ed.getValueAsString().equals(vs.getUrl())) {
            if (first) {
              first = false;
              b.append("<ul>\r\n");
            }
            b.append(" <li>"+translate("vs.usage", "Excluded from ")+"<a href=\""+vc.getUserString("path")+"\">"+Utilities.escapeXml(gt(vc.getNameElement()))+"</a></li>\r\n");
            break;
          }
        }
      }
    }
    List<String> distinctUrls = new ArrayList<String>();
    for (String url : sdurls) {
      if (!distinctUrls.contains(url))
        distinctUrls.add(url);
    }
    for (String url : distinctUrls) {
      StructureDefinition sd = context.fetchResource(StructureDefinition.class, url);
      for (ElementDefinition ed : sd.getSnapshot().getElement()) {
        if (ed.hasBinding() && ed.getBinding().hasValueSet()) {
          if ((ed.getBinding().hasValueSetUriType() && ed.getBinding().getValueSetUriType().getValueAsString().equals(vs.getUrl())) ||
              (ed.getBinding().hasValueSetReference() && ed.getBinding().getValueSetReference().getReference().equals(vs.getUrl()))) {
            if (first) {
              first = false;
              b.append("<ul>\r\n");
            }
            b.append(" <li><a href=\""+sd.getUserString("path")+"\">"+Utilities.escapeXml(sd.getName())+"</a></li>\r\n");
            break;
          }
        }
      }
    }
    
    if (first)
      b.append("<p>"+translate("vs.usage", "This value set is not used")+"</p>\r\n");
    else
      b.append("</ul>\r\n");
    return b.toString();
  }

  
}
