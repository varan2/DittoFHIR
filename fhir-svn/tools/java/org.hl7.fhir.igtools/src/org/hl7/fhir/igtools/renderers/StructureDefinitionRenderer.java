package org.hl7.fhir.igtools.renderers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.conformance.ProfileUtilities;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.formats.IParser.OutputStyle;
import org.hl7.fhir.r4.formats.XmlParser;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.ElementDefinition.ElementDefinitionBindingComponent;
import org.hl7.fhir.r4.model.ElementDefinition.ElementDefinitionConstraintComponent;
import org.hl7.fhir.r4.model.ElementDefinition.ElementDefinitionExampleComponent;
import org.hl7.fhir.r4.model.ElementDefinition.ElementDefinitionMappingComponent;
import org.hl7.fhir.r4.model.ElementDefinition.ElementDefinitionSlicingComponent;
import org.hl7.fhir.r4.model.ElementDefinition.ElementDefinitionSlicingDiscriminatorComponent;
import org.hl7.fhir.r4.model.ElementDefinition.SlicingRules;
import org.hl7.fhir.r4.model.ElementDefinition.TypeRefComponent;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.Enumerations.BindingStrength;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureDefinition.StructureDefinitionMappingComponent;
import org.hl7.fhir.r4.model.StructureDefinition.TypeDerivationRule;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.utils.ToolingExtensions;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.igtools.publisher.FetchedFile;
import org.hl7.fhir.igtools.publisher.FetchedResource;
import org.hl7.fhir.igtools.publisher.IGKnowledgeProvider;
import org.hl7.fhir.igtools.publisher.SpecMapManager;
import org.hl7.fhir.utilities.CommaSeparatedStringBuilder;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.TranslatingUtilities.TranslationServices;
import org.hl7.fhir.utilities.xhtml.XhtmlComposer;

public class StructureDefinitionRenderer extends BaseRenderer {
  public static final String RIM_MAPPING = "http://hl7.org/v3";
  public static final String v2_MAPPING = "http://hl7.org/v2";
  public static final String LOINC_MAPPING = "http://loinc.org";
  public static final String SNOMED_MAPPING = "http://snomed.info";

  ProfileUtilities utils;
  private StructureDefinition sd;
  private String destDir;

  public StructureDefinitionRenderer(IWorkerContext context, String prefix, StructureDefinition sd, String destDir, IGKnowledgeProvider igp, List<SpecMapManager> maps) {
    super(context, prefix, igp, maps);
    this.sd = sd;
    this.destDir = destDir;
    utils = new ProfileUtilities(context, null, igp);
    utils.setIgmode(true);
  }

  @Override
  public void setTranslator(org.hl7.fhir.utilities.TranslatingUtilities.TranslationServices translator) {
    super.setTranslator(translator);
    utils.setTranslator(translator);
  }

  public String summary() {
    try {
      if (sd.getDifferential() == null)
        return "<p>"+translate("sd.summary", "No Summary, as this profile has no differential")+"</p>";

      // references
      List<String> refs = new ArrayList<String>(); // profile references
      // extensions (modifier extensions)
      List<String> ext = new ArrayList<String>(); // extensions
      // slices
      List<String> slices = new ArrayList<String>(); // Fixed Values 
      // numbers - must support, required, prohibited, fixed
      int supports = 0;
      int requiredOutrights = 0;
      int requiredNesteds = 0;
      int fixeds = 0;
      int prohibits = 0;

      for (ElementDefinition ed : sd.getDifferential().getElement()) {
        if (ed.getPath().contains(".")) {
          if (ed.getMin() == 1)
            if (parentChainHasOptional(ed, sd))
              requiredNesteds++;
            else
              requiredOutrights++;
          if ("0".equals(ed.getMax()))
            prohibits++;
          if (ed.getMustSupport())
            supports++;
          if (ed.hasFixed())
            fixeds++;

          for (TypeRefComponent t : ed.getType()) {
            if (t.hasProfile() && !igp.isDatatype(t.getProfile().substring(40))) {
              if (ed.getPath().endsWith(".extension"))
                tryAdd(ext, summariseExtension(t.getProfile(), false, prefix));
              else if (ed.getPath().endsWith(".modifierExtension"))
                tryAdd(ext, summariseExtension(t.getProfile(), true, prefix));
              else
                tryAdd(refs, describeProfile(t.getProfile(), prefix));
            } 
            if (t.hasTargetProfile()) {
              tryAdd(refs, describeProfile(t.getTargetProfile(), prefix));
            } 
          }

          if (ed.hasSlicing() && !ed.getPath().endsWith(".extension") && !ed.getPath().endsWith(".modifierExtension"))
            tryAdd(slices, describeSlice(ed.getPath(), ed.getSlicing()));
        }
      }
      StringBuilder res = new StringBuilder("<a name=\"summary\"> </a>\r\n<p><b>\r\n"+translate("sd.summary", "Summary")+"\r\n</b></p>\r\n");
      if (ToolingExtensions.hasExtension(sd, "http://hl7.org/fhir/StructureDefinition/structuredefinition-summary")) {
        Extension v = ToolingExtensions.getExtension(sd, "http://hl7.org/fhir/StructureDefinition/structuredefinition-summary");
        res.append(processMarkdown("Profile.summary", (PrimitiveType) v.getValue()));
      }
      if (supports + requiredOutrights + requiredNesteds + fixeds + prohibits > 0) {
        boolean started = false;
        res.append("<p>");
        if (requiredOutrights > 0 || requiredNesteds > 0) {
          started = true;
          res.append(translate("sd.summary", "Mandatory: %s %s", toStr(requiredOutrights), (requiredOutrights > 1 ? translate("sd.summary", Utilities.pluralizeMe("element")) : translate("sd.summary", "element"))));
          if (requiredNesteds > 0)
            res.append(translate("sd.summary", " (%s nested mandatory %s)", toStr(requiredNesteds), requiredNesteds > 1 ? translate("sd.summary", Utilities.pluralizeMe("element")) : translate("sd.summary", "element"))); 
        }
        if (supports > 0) {
          if (started)
            res.append("<br/> ");
          started = true;
          res.append(translate("sd.summary", "Must-Support: %s %s", toStr(supports), supports > 1 ? translate("sd.summary", Utilities.pluralizeMe("element")) : translate("sd.summary", "element"))); 
        }
        if (fixeds > 0) {
          if (started)
            res.append("<br/> ");
          started = true;
          res.append(translate("sd.summary", "Fixed Value: %s %s", toStr(fixeds), fixeds > 1 ? translate("sd.summary", Utilities.pluralizeMe("element")) : translate("sd.summary", "element"))); 
        }
        if (prohibits > 0) {
          if (started)
            res.append("<br/> ");
          started = true;
          res.append(translate("sd.summary", "Prohibited: %s %s", toStr(prohibits), prohibits > 1 ? translate("sd.summary", Utilities.pluralizeMe("element")) : translate("sd.summary", "element"))); 
        }
        res.append("</p>");        
      }
      if (!refs.isEmpty()) {
        res.append("<p><b>"+translate("sd.summary", "Structures")+"</b></p>\r\n<p>"+translate("sd.summary", "This structure refers to these other structures")+":</p>\r\n<ul>\r\n");
        for (String s : refs)
          res.append(s);
        res.append("\r\n</ul>\r\n\r\n");
      }
      if (!ext.isEmpty()) {
        res.append("<p><b>"+translate("sd.summary", "Extensions")+"</b></p>\r\n<p>"+translate("sd.summary", "This structure refers to these extensions")+":</p>\r\n<ul>\r\n");
        for (String s : ext)
          res.append(s);
        res.append("\r\n</ul>\r\n\r\n");
      }
      if (!slices.isEmpty()) {
        res.append("<p><b>"+translate("sd.summary", "Slices")+"</b></p>\r\n<p>"+translate("sd.summary", "This structure defines the following %sSlices%s", "<a href=\""+prefix+"profiling.html#slices\">", "</a>")+":</p>\r\n<ul>\r\n");
        for (String s : slices)
          res.append(s);
        res.append("\r\n</ul>\r\n\r\n");
      }
      return res.toString();
    } catch (Exception e) {
      return "<p><i>"+Utilities.escapeXml(e.getMessage())+"</i></p>";
    }
  }



  private boolean parentChainHasOptional(ElementDefinition ed, StructureDefinition profile) {
    if (!ed.getPath().contains("."))
      return false;

    ElementDefinition match = (ElementDefinition) ed.getUserData(ProfileUtilities.DERIVATION_POINTER);
    if (match == null)
      return true; // really, we shouldn't get here, but this appears to be common in the existing profiles?  
    // throw new Error("no matches for "+ed.getPath()+"/"+ed.getName()+" in "+profile.getUrl());

    while (match.getPath().contains(".")) {
      if (match.getMin() == 0) {
        return true;
      }
      match = getElementParent(profile.getSnapshot().getElement(), match);
    }

    return false;
  }

  private ElementDefinition getElementParent(List<ElementDefinition> list, ElementDefinition element) {
    String targetPath = element.getPath().substring(0, element.getPath().lastIndexOf("."));
    int index = list.indexOf(element) - 1;
    while (index >= 0) {
      if (list.get(index).getPath().equals(targetPath))
        return list.get(index);
      index--;
    }
    return null;
  }

  private String describeSlice(String path, ElementDefinitionSlicingComponent slicing) {
    if (!slicing.hasDiscriminator())
      return "<li>"+translate("sd.summary", "There is a slice with no discriminator at %s", path)+"</li>\r\n";
    String s = "";
    if (slicing.getOrdered())
      s = "ordered";
    if (slicing.getRules() != SlicingRules.OPEN)
      s = Utilities.noString(s) ? slicing.getRules().getDisplay() : s+", "+ slicing.getRules().getDisplay();
      if (!Utilities.noString(s))
        s = " ("+s+")";
      CommaSeparatedStringBuilder b = new CommaSeparatedStringBuilder();
      for (ElementDefinitionSlicingDiscriminatorComponent d : slicing.getDiscriminator()) 
        b.append(d.getType().toCode()+":"+d.getPath());
      if (slicing.getDiscriminator().size() == 1)
        return "<li>"+translate("sd.summary", "The element %s is sliced based on the value of %s", path, b.toString())+s+"</li>\r\n";
      else
        return "<li>"+translate("sd.summary", "The element %s is sliced based on the values of %s", path, b.toString())+s+"</li>\r\n";
  }

  private void tryAdd(List<String> ext, String s) {
    if (!Utilities.noString(s) && !ext.contains(s))
      ext.add(s);
  }

  private String summariseExtension(String url, boolean modifier, String prefix) throws Exception {
    StructureDefinition ed = context.fetchResource(StructureDefinition.class, url);
    if (ed == null)
      return "<li>"+translate("sd.summary", "Unable to summarise extension %s (no extension found)", url)+"</li>";
    if (ed.getUserData("path") == null)
      return "<li><a href=\""+"extension-"+ed.getId().toLowerCase()+".html\">"+url+"</a>"+(modifier ? " (<b>"+translate("sd.summary", "Modifier")+"</b>) " : "")+"</li>\r\n";    
    else
      return "<li><a href=\""+ed.getUserString("path")+"\">"+url+"</a>"+(modifier ? " (<b>"+translate("sd.summary", "Modifier")+"</b>) " : "")+"</li>\r\n";    
  }

  private String describeProfile(String url, String prefix) throws Exception {
    if (url.startsWith("http://hl7.org/fhir/StructureDefinition/") && (igp.isDatatype(url.substring(40)) || igp.isResource(url.substring(40)) || "Resource".equals(url.substring(40))))
      return null;

    StructureDefinition ed = context.fetchResource(StructureDefinition.class, url);
    if (ed == null)
      return "<li>"+translate("sd.summary", "unable to summarise profile %s (no profile found)",url)+"</li>";
    return "<li><a href=\""+ed.getUserString("path")+"\">"+url+"</a></li>\r\n";    
  }

  private String describeReference(ElementDefinitionBindingComponent binding) {
    if (binding.getValueSet() instanceof UriType) {
      UriType uri = (UriType) binding.getValueSet();
      return "<a href=\""+uri.asStringValue()+"\">"+uri.asStringValue()+"</a>";
    } if (binding.getValueSet() instanceof Reference) {
      Reference ref = (Reference) binding.getValueSet();
      String disp = gt(ref.getDisplayElement());
      ValueSet vs = context.fetchResource(ValueSet.class, ref.getReference());
      if (disp == null && vs != null)
        disp = vs.getName();
      return "<a href=\""+(vs == null ? ref.getReference() : vs.getUserData("filename"))+"\">"+disp+"</a>";
    }
    else
      return "??";
  }

  private String summariseValue(Type fixed) throws Exception {
    if (fixed instanceof org.hl7.fhir.r4.model.PrimitiveType)
      return ((org.hl7.fhir.r4.model.PrimitiveType) fixed).asStringValue();
    if (fixed instanceof CodeableConcept) 
      return summarise((CodeableConcept) fixed);
    if (fixed instanceof Quantity) 
      return summarise((Quantity) fixed);
    throw new Exception("Generating text summary of fixed value not yet done for type "+fixed.getClass().getName());
  }


  private String summarise(Quantity quantity) {
    String cu = "";
    if ("http://unitsofmeasure.org/".equals(quantity.getSystem()))
      cu = " ("+translate("sd.summary", "UCUM")+": "+quantity.getCode()+")";
    if ("http://snomed.info/sct".equals(quantity.getSystem()))
      cu = " ("+translate("sd.summary", "SNOMED CT")+": "+quantity.getCode()+")";
    return quantity.getValue().toString()+quantity.getUnit()+cu;
  }

  private String summarise(CodeableConcept cc) throws Exception {
    if (cc.getCoding().size() == 1 && cc.getText() == null) {
      return summarise(cc.getCoding().get(0));
    } else if (cc.getCoding().size() == 0 && cc.hasText()) {
      return "\"" + cc.getText()+"\"";
    } else 
      throw new Exception("too complex to describe");
  }

  private String summarise(Coding coding) throws Exception {
    if ("http://snomed.info/sct".equals(coding.getSystem()))
      return ""+translate("sd.summary", "SNOMED CT code")+" "+coding.getCode()+ (!coding.hasDisplay() ? "" : "(\""+gt(coding.getDisplayElement())+"\")");
    if ("http://loinc.org".equals(coding.getSystem()))
      return ""+translate("sd.summary", "LOINC code")+" "+coding.getCode()+ (!coding.hasDisplay() ? "" : "(\""+gt(coding.getDisplayElement())+"\")");
    CodeSystem cs = context.fetchCodeSystem(coding.getSystem());
    if (cs !=  null) {
      return "<a href=\""+cs.getUserData("filename")+"#"+coding.getCode()+"\">"+coding.getCode()+"</a>"+(!coding.hasDisplay() ? "" : "(\""+gt(coding.getDisplayElement())+"\")");
    }
    throw new Exception("Unknown system "+coding.getSystem()+" generating fixed value description");
  }

  private String root(String path) {
    return path.contains(".") ? path.substring(0, path.lastIndexOf('.')) : path;
  }

  public String diff(String defnFile) throws IOException, FHIRException, org.hl7.fhir.exceptions.FHIRException {
    if (sd.getDifferential().getElement().isEmpty())
      return "";
    else
      return new XhtmlComposer().compose(utils.generateTable(defnFile, sd, true, destDir, false, sd.getId(), false, prefix, "", false, false));
  }

  public String snapshot(String defnFile) throws IOException, FHIRException, org.hl7.fhir.exceptions.FHIRException {
    if (sd.getSnapshot().getElement().isEmpty())
      return "";
    else
      return new XhtmlComposer().compose(utils.generateTable(defnFile, sd, false, destDir, false, sd.getId(), true, prefix, "", false, false));
  }

  public String grid(String defnFile) throws IOException, FHIRException, org.hl7.fhir.exceptions.FHIRException {
    if (sd.getSnapshot().getElement().isEmpty())
      return "";
    else
      return new XhtmlComposer().compose(utils.generateGrid(defnFile, sd, destDir, false, sd.getId(), prefix, ""));
  }

  public String tx() {
    List<String> txlist = new ArrayList<String>();
    Map<String, ElementDefinitionBindingComponent> txmap = new HashMap<String, ElementDefinitionBindingComponent>();
    for (ElementDefinition ed : sd.getSnapshot().getElement()) {
      if (ed.hasBinding() && !"0".equals(ed.getMax())) {
        String path = ed.getPath();
        if (ed.getType().size() == 1 && ed.getType().get(0).getCode().equals("Extension"))
          path = path + "<br/>"+ed.getType().get(0).getProfile();
        txlist.add(path);
        txmap.put(path, ed.getBinding());
      }
    }
    if (txlist.isEmpty())
      return "";
    else {
      StringBuilder b = new StringBuilder();
      b.append("<h4>"+translate("sd.tx", "Terminology Bindings")+"</h4>\r\n");       
      b.append("<table class=\"list\">\r\n");
      b.append("<tr><td><b>"+translate("sd.tx", "Path")+"</b></td><td><b>"+translate("sd.tx", "Name")+"</b></td><td><b>"+translate("sd.tx", "Conformance")+"</b></td><td><b>"+translate("sd.tx", "ValueSet")+"</b></td></tr>\r\n");
      for (String path : txlist)  {
        ElementDefinitionBindingComponent tx = txmap.get(path);
        String vss = "";
        String vsn = "?ext";
        if (tx.hasValueSet()) {
          if (tx.getValueSet() instanceof UriType) {
            vss = "<a href=\""+((UriType)tx.getValueSet()).asStringValue()+"\">"+Utilities.escapeXml(((UriType)tx.getValueSet()).asStringValue())+"</a>";
          } else {
            String uri = ((Reference)tx.getValueSet()).getReference();
            ValueSet vs = context.fetchResource(ValueSet.class, canonicalise(uri));
            if (vs == null)
              vss = "<a href=\""+prefix+uri+"\">"+Utilities.escapeXml(uri)+"</a>";
            else { 
              String p = vs.getUserString("path");
              if (p == null)
                vss = "<a href=\"??\">"+Utilities.escapeXml(gt(vs.getNameElement()))+" ("+translate("sd.tx", "missing link")+")</a>";
              else if (p.startsWith("http:"))
                vss = "<a href=\""+p+"\">"+Utilities.escapeXml(gt(vs.getNameElement()))+"</a>";
              else
                vss = "<a href=\""+p+"\">"+Utilities.escapeXml(gt(vs.getNameElement()))+"</a>";
              vsn = gt(vs.getNameElement());
            }
          }
        }
        b.append("<tr><td>").append(path).append("</td><td>").append(Utilities.escapeXml(vsn)).append("</td><td><a href=\"").
        append(prefix).append("terminologies.html#").append(tx.getStrength() == null ? "" : egt(tx.getStrengthElement())).
        append("\">").append(tx.getStrength() == null ? "" : egt(tx.getStrengthElement())).append("</a></td><td>").append(vss).append("</td></tr>\r\n");
      }
      b.append("</table>\r\n");
      return b.toString();

    }
  }

  public String inv() {
    List<String> txlist = new ArrayList<String>();
    Map<String, List<ElementDefinitionConstraintComponent>> txmap = new HashMap<String, List<ElementDefinitionConstraintComponent>>();
    for (ElementDefinition ed : sd.getSnapshot().getElement()) {
      if (!"0".equals(ed.getMax())) {
        txlist.add(ed.getPath());
        txmap.put(ed.getPath(), ed.getConstraint());
      }
    }
    if (txlist.isEmpty())
      return "";
    else {
      StringBuilder b = new StringBuilder();
      b.append("<h4>"+translate("sd.inv", "Constraints")+"</h4>\r\n");       
      b.append("<table class=\"list\">\r\n");
      b.append("<tr><td width=\"60\"><b>"+translate("sd.inv", "Id")+"</b></td><td><b>"+translate("sd.inv", "Path")+"</b></td><td><b>"+translate("sd.inv", "Details")+"</b></td><td><b>"+translate("sd.inv", "Requirements")+"</b></td></tr>\r\n");
      for (String path : txlist)  {
        List<ElementDefinitionConstraintComponent> invs = txmap.get(path);
        for (ElementDefinitionConstraintComponent inv : invs) {
          b.append("<tr><td>").append(inv.getKey()).append("</td><td>").append(path).append("</td><td>").append(Utilities.escapeXml(gt(inv.getHumanElement())))
          .append("<br/>: ").append(Utilities.escapeXml(inv.getExpression())).append("</td><td>").append(Utilities.escapeXml(gt(inv.getRequirementsElement()))).append("</td></tr>\r\n");
        }
      }
      b.append("</table>\r\n");
      return b.toString();      
    }
  }

  public class StringPair {

    private String match;
    private String replace;

    public StringPair(String match, String replace) {
      this.match = match;
      this.replace = replace;
    }

  }

  public String dict() throws Exception {
    int i = 1;
    StringBuilder b = new StringBuilder();
    b.append("<table class=\"dict\">\r\n");

    List<StringPair> replacements = new ArrayList<StringPair>();
    for (ElementDefinition ec : sd.getSnapshot().getElement()) {
      if (isProfiledExtension(ec)) {
        StructureDefinition extDefn = context.fetchResource(StructureDefinition.class, ec.getType().get(0).getProfile());
        if (extDefn == null) {
          String title = ec.getPath() + " ("+(ec.getType().get(0).getProfile().startsWith("#") ? sd.getUrl() : "")+ec.getType().get(0).getProfile()+")";
          b.append("  <tr><td colspan=\"2\" class=\"structure\"><a name=\""+ec.getId()+"\"> </a><b>"+title+"</b></td></tr>\r\n");
          generateElementInner(b, sd,  ec, 1, null);
        } else {
          String title = ec.getPath() + " (<a href=\""+(extDefn.hasUserData("path") ? extDefn.getUserData("path") : "extension-"+extDefn.getId().toLowerCase()+".html")+
              "\">"+(ec.getType().get(0).getProfile().startsWith("#") ? sd.getUrl() : "")+ec.getType().get(0).getProfile()+"</a>)";
          b.append("  <tr><td colspan=\"2\" class=\"structure\"><a name=\""+ec.getId()+"\"> </a>");
          if (ec.getId().endsWith("[x]")) {
            Set<String> tl = new HashSet<String>();
            for (TypeRefComponent tr : ec.getType()) {
              String tc = tr.getCode();
              if (!tl.contains(tc)) {
                tl.add(tc);
                String s = ec.getId().replace("[x]", Utilities.capitalize(tc));
                b.append("<a name=\""+s+"\"> </a>");
                replacements.add(new StringPair(ec.getId(), s));
              }
            }
          }
          b.append("<b>"+title+"</b></td></tr>\r\n");
          ElementDefinition valueDefn = getExtensionValueDefinition(extDefn);
          generateElementInner(b, extDefn, extDefn.getSnapshot().getElement().get(0), valueDefn == null ? 2 : 3, valueDefn);
        }
      } else {
        String title = ec.getPath() + (!ec.hasSliceName() ? "" : "(" +ec.getSliceName() +")");
        b.append("  <tr><td colspan=\"2\" class=\"structure\"><a name=\""+ec.getId()+"\"> </a>");
        if (ec.getId().endsWith("[x]")) {
          Set<String> tl = new HashSet<String>();
          for (TypeRefComponent tr : ec.getType()) {
            String tc = tr.getCode();
            if (!tl.contains(tc)) {
              tl.add(tc);
              String s = ec.getId().replace("[x]", Utilities.capitalize(tc));
              b.append("<a name=\""+s+"\"> </a>");
              replacements.add(new StringPair(ec.getId(), s));
            }
          }
        } else if (ec.hasBase() && ec.getBase().getPath().endsWith("[x]")) {
          String s = nottail(ec.getId())+"."+tail(ec.getBase().getPath());
          replacements.add(new StringPair(ec.getId(), s));
          b.append("<a name=\""+s+"\"> </a>");
        } else if (!ec.getId().equals(ec.getPath())) {
          b.append("<a name=\""+ec.getPath()+"\"> </a>");
        }
        for (StringPair s : replacements)
          if (ec.getId().startsWith(s.match))
            b.append("<a name=\""+s.replace+ec.getId().substring(s.match.length())+"\"> </a>");
        b.append("<b>"+title+"</b></td></tr>\r\n");
        generateElementInner(b, sd, ec, 1, null);
        if (ec.hasSlicing())
          generateSlicing(sd, ec.getSlicing());
      }
    }
    b.append("</table>\r\n");
    i++;
    return b.toString();
  }

  private boolean isProfiledExtension(ElementDefinition ec) {
    return ec.getType().size() == 1 && "Extension".equals(ec.getType().get(0).getCode()) && ec.getType().get(0).hasProfile();
  }


  private String makePathLink(ElementDefinition element) {
    return element.getId();
  }

  private ElementDefinition getExtensionValueDefinition(StructureDefinition extDefn) {
    for (ElementDefinition ed : extDefn.getSnapshot().getElement()) {
      if (ed.getPath().startsWith("Extension.value"))
        return ed;
    }
    return null;
  }

  private void generateElementInner(StringBuilder b, StructureDefinition profile, ElementDefinition d, int mode, ElementDefinition value) throws Exception {
    tableRowNE(b, translate("sd.dict", "Definition"), null, processMarkdown(profile.getName(), d.getDefinitionElement()));
    tableRowNE(b, translate("sd.dict", "Note"), null, businessIdWarning(profile.getName(), tail(d.getPath())));
    tableRow(b, translate("sd.dict", "Control"), "conformance-rules.html#conformance", describeCardinality(d) + summariseConditions(d.getCondition()));
    tableRowNE(b, translate("sd.dict", "Binding"), "terminologies.html", describeBinding(d));
    if (d.hasContentReference())
      tableRow(b, translate("sd.dict", "Type"), null, "See "+d.getContentReference().substring(1));
    else
      tableRowNE(b, translate("sd.dict", "Type"), "datatypes.html", describeTypes(d.getType()) + processSecondary(mode, value));
    if (d.getPath().endsWith("[x]"))
      tableRowNE(b, translate("sd.dict", "[x] Note"), null, translate("sd.dict", "See %sChoice of Data Types%s for further information about how to use [x]", "<a href=\""+prefix+"formats.html#choice\">", "</a>"));
    tableRow(b, translate("sd.dict", "Is Modifier"), "conformance-rules.html#ismodifier", displayBoolean(d.getIsModifier()));
    tableRow(b, translate("sd.dict", "Must Support"), "conformance-rules.html#mustSupport", displayBoolean(d.getMustSupport()));
    tableRowNE(b, translate("sd.dict", "Requirements"),  null, processMarkdown(profile.getName(), d.getRequirementsElement()));
    tableRowHint(b, translate("sd.dict", "Alternate Names"), translate("sd.dict", "Other names by which this resource/element may be known"), null, describeAliases(d.getAlias()));
    tableRowNE(b, translate("sd.dict", "Comments"),  null, processMarkdown(profile.getName(), d.getCommentElement()));
    tableRow(b, translate("sd.dict", "Max Length"), null, !d.hasMaxLengthElement() ? null : toStr(d.getMaxLength()));
    tableRowNE(b, translate("sd.dict", "Default Value"), null, encodeValue(d.getDefaultValue()));
    tableRowNE(b, translate("sd.dict", "Meaning if Missing"), null, d.getMeaningWhenMissing());
    tableRowNE(b, translate("sd.dict", "Fixed Value"), null, encodeValue(d.getFixed()));
    tableRowNE(b, translate("sd.dict", "Pattern Value"), null, encodeValue(d.getPattern()));
    tableRowNE(b, translate("sd.dict", "Example"), null, encodeValues(d.getExample()));
    tableRowNE(b, translate("sd.dict", "Invariants"), null, invariants(d.getConstraint()));
    tableRow(b, translate("sd.dict", "LOINC Code"), null, getMapping(profile, d, LOINC_MAPPING));
    tableRow(b, translate("sd.dict", "SNOMED-CT Code"), null, getMapping(profile, d, SNOMED_MAPPING));
  }

  private void generateSlicing(StructureDefinition profile, ElementDefinitionSlicingComponent slicing) throws IOException {
    StringBuilder b = new StringBuilder();
    if (slicing.getOrdered())
      b.append("<li>"+translate("sd.dict", "ordered")+"</li>");
    else
      b.append("<li>"+translate("sd.dict", "unordered")+"</li>");
    if (slicing.hasRules())
      b.append("<li>"+slicing.getRules().getDisplay()+"</li>");
    if (!slicing.getDiscriminator().isEmpty()) {
      b.append("<li>"+translate("sd.dict", "discriminators")+": ");
      boolean first = true;
      for (ElementDefinitionSlicingDiscriminatorComponent s : slicing.getDiscriminator()) {
        if (first)
          first = false;
        else
          b.append(", ");
        b.append(s.getType().toCode()+":"+s.getPath());
      }
      b.append("</li>");
    }
    tableRowNE(b, ""+translate("sd.dict", "Slicing"), "profiling.html#slicing", translate("sd.dict", "This element introduces a set of slices. The slicing rules are")+": <ul> "+b.toString()+"</ul>");
  }

  private void tableRow(StringBuilder b, String name, String defRef, String value) throws IOException {
    if (value != null && !"".equals(value)) {
      if (defRef != null) 
        b.append("  <tr><td><a href=\""+prefix+defRef+"\">"+name+"</a></td><td>"+Utilities.escapeXml(value)+"</td></tr>\r\n");
      else
        b.append("  <tr><td>"+name+"</td><td>"+Utilities.escapeXml(value)+"</td></tr>\r\n");
    }
  }


  private void tableRowHint(StringBuilder b, String name, String hint, String defRef, String value) throws IOException {
    if (value != null && !"".equals(value)) {
      if (defRef != null) 
        b.append("  <tr><td><a href=\""+prefix+defRef+"\" title=\""+Utilities.escapeXml(hint)+"\">"+name+"</a></td><td>"+Utilities.escapeXml(value)+"</td></tr>\r\n");
      else
        b.append("  <tr><td title=\""+Utilities.escapeXml(hint)+"\">"+name+"</td><td>"+Utilities.escapeXml(value)+"</td></tr>\r\n");
    }
  }


  private void tableRowNE(StringBuilder b, String name, String defRef, String value) throws IOException {
    if (value != null && !"".equals(value))
      if (defRef != null) 
        b.append("  <tr><td><a href=\""+prefix+defRef+"\">"+name+"</a></td><td>"+value+"</td></tr>\r\n");
      else
        b.append("  <tr><td>"+name+"</td><td>"+value+"</td></tr>\r\n");
  }

  private String head(String path) {
    if (path.contains("."))
      return path.substring(0, path.indexOf("."));
    else
      return path;
  }

  private String tail(String path) {
    if (path.contains("."))
      return path.substring(path.lastIndexOf(".")+1);
    else
      return path;
  }

  private String nottail(String path) {
    if (path.contains("."))
      return path.substring(0, path.lastIndexOf("."));
    else
      return path;
  }

  private String businessIdWarning(String resource, String name) {
    if (name.equals("identifier"))
      return ""+translate("sd.dict", "This is a business identifer, not a resource identifier (see %sdiscussion%s)", "<a href=\""+prefix+"resource.html#identifiers\">", "</a>");
    if (name.equals("version")) // && !resource.equals("Device"))
      return ""+translate("sd.dict", "This is a business versionId, not a resource version id (see %sdiscussion%s)", "<a href=\""+prefix+"resource.html#versions\">", "</a>");
    return null;
  }

  private String describeCardinality(ElementDefinition d) {
    if (!d.hasMax() && d.getMinElement() == null)
      return "";
    else if (d.getMax() == null)
      return toStr(d.getMin()) + "..?";
    else
      return toStr(d.getMin()) + ".." + d.getMax();
  }

  private String summariseConditions(List<IdType> conditions) {
    if (conditions.isEmpty())
      return "";
    else {
      CommaSeparatedStringBuilder b = new CommaSeparatedStringBuilder();
      for (IdType t : conditions)
        b.append(t.getValue());
      return " "+translate("sd.dict", "This element is affected by the following invariants")+": "+b.toString();
    }
  }

  private String describeTypes(List<TypeRefComponent> types) throws Exception {
    if (types.isEmpty())
      return "";

    StringBuilder b = new StringBuilder();
    if (types.size() == 1)
      describeType(b, types.get(0));
    else {
      boolean first = true;
      b.append(translate("sd.dict", "Choice of")+": ");
      for (TypeRefComponent t : types) {
        if (first)
          first = false;
        else
          b.append(", ");
        describeType(b, t);
      }
    }
    return b.toString();
  }

  private void describeType(StringBuilder b, TypeRefComponent t) throws Exception {
    if (t.getCode() == null)
      return;
    if (t.getCode().startsWith("="))
      return;

    if (t.getCode().startsWith("xs:")) {
      b.append(t.getCode());
    } else {
      b.append("<a href=\"");
      String s = igp.getLinkFor("", t.getCode());
//    GG 13/12/2016 - I think that this is always wrong now. 
//      if (!s.startsWith("http:") && !s.startsWith("https:") && !s.startsWith(".."))
//        b.append(prefix);         
      b.append(s);
      if (!s.contains(".html")) {
 //     b.append(".html#");
 //     String type = t.getCode();
 //     if (type.equals("*"))
 //       b.append("open");
 //     else 
 //       b.append(t.getCode());
      }
      b.append("\">");
      b.append(t.getCode());
      b.append("</a>");
    }
    if (t.hasProfile()) {
      b.append("(");
      StructureDefinition p = context.fetchResource(StructureDefinition.class, t.getProfile());
      if (p == null)
        b.append(t.getProfile());
      else {
        String pth = p.getUserString("path");
        b.append("<a href=\""+pth+"\" title=\""+t.getProfile()+"\">");
        b.append(p.getName());
        b.append("</a>");
      }
      b.append(")");
    }
    if (t.hasTargetProfile()) {
      b.append("(");
      StructureDefinition p = context.fetchResource(StructureDefinition.class, t.getTargetProfile());
      if (p == null)
        b.append(t.getTargetProfile());
      else {
        String pth = p.getUserString("path");
        b.append("<a href=\""+pth+"\" title=\""+t.getTargetProfile()+"\">");
        b.append(p.getName());
        b.append("</a>");
      }
      b.append(")");
    }
  }

  private String processSecondary(int mode, ElementDefinition value) throws Exception {
    switch (mode) {
    case 1 : return "";
    case 2 : return "  ("+translate("sd.dict", "Complex Extension")+")";
    case 3 : return "  ("+translate("sd.dict", "Extension Type")+": "+describeTypes(value.getType())+")";
    default: return "";
    }
  }

  private String displayBoolean(boolean mustUnderstand) {
    if (mustUnderstand)
      return "true";
    else
      return null;
  }

  private String invariants(List<ElementDefinitionConstraintComponent> constraints) {
    if (constraints.isEmpty())
      return null;
    StringBuilder s = new StringBuilder();
    if (constraints.size() > 0) {
      s.append("<b>"+translate("sd.dict", "Defined on this element")+"</b><br/>\r\n");
      List<String> ids = new ArrayList<String>();
      for (ElementDefinitionConstraintComponent id : constraints)
        ids.add(id.hasKey() ? id.getKey() : id.toString());
      Collections.sort(ids);
      boolean b = false;
      for (String id : ids) {
        ElementDefinitionConstraintComponent inv = getConstraint(constraints, id);
        if (b)
          s.append("<br/>");
        else
          b = true;
        s.append("<b title=\""+translate("sd.dict", "Formal Invariant Identifier")+"\">"+id+"</b>: "+Utilities.escapeXml(gt(inv.getHumanElement()))+" (: "+Utilities.escapeXml(inv.getExpression())+")");
      }
    }

    return s.toString();
  }

  private ElementDefinitionConstraintComponent getConstraint(List<ElementDefinitionConstraintComponent> constraints, String id) {
    for (ElementDefinitionConstraintComponent c : constraints) {
      if (c.hasKey() && c.getKey().equals(id))
        return c;
      if (!c.hasKey() && c.toString().equals(id))
        return c;
    } 
    return null;
  }



  private String describeBinding(ElementDefinition d) throws Exception {
    if (!d.hasBinding())
      return null;
    else {
      // return TerminologyNotesGenerator.describeBinding(prefix, d.getBinding(), page);
      ElementDefinitionBindingComponent def = d.getBinding();
      if (!def.hasValueSet()) 
        return def.getDescription();
      String ref = def.getValueSet() instanceof UriType ? ((UriType) def.getValueSet()).asStringValue() : ((Reference) def.getValueSet()).getReference();
      ValueSet vs = context.fetchResource(ValueSet.class, canonicalise(ref));
      if (vs != null) {
        String pp = (String) vs.getUserData("path");
        if (pp == null) {
          return null;
        } else if (pp.startsWith("http:") || pp.startsWith("https:"))
          return def.getDescription()+"<br/>"+conf(def)+ "<a href=\""+pp+"\">"+gt(vs.getNameElement())+"</a>"+confTail(def);
        else
          return def.getDescription()+"<br/>"+conf(def)+ "<a href=\""+pp.replace(File.separatorChar, '/')+"\">"+vs.getName()+"</a>"+confTail(def);
      }
      if (ref.startsWith("http:") || ref.startsWith("https:"))
        return def.getDescription()+"<br/>"+conf(def)+" <a href=\""+ref+"\">"+ref+"</a>"+confTail(def);
      else
        return def.getDescription()+"<br/>"+conf(def)+" ?? Broken Reference to "+ref+" ??"+confTail(def);

    }
  }

  private String confTail(ElementDefinitionBindingComponent def) {
    if (def.getStrength() == BindingStrength.EXTENSIBLE)
      return "; "+translate("sd.dict", "other codes may be used where these codes are not suitable");
    else
      return "";
  }

  private String conf(ElementDefinitionBindingComponent def) {
    switch (def.getStrength()) {
    case EXAMPLE:
      return ""+translate("sd.dict", "For example codes, see ");
    case PREFERRED:
      return ""+translate("sd.dict", "The codes SHOULD be taken from ");
    case EXTENSIBLE:
      return ""+translate("sd.dict", "The codes SHALL be taken from ");
    case REQUIRED:
      return ""+translate("sd.dict", "The codes SHALL be taken from ");
    default:
      return ""+"??";
    }
  }

  private String encodeValues(List<ElementDefinitionExampleComponent> examples) throws Exception {
    StringBuilder b = new StringBuilder();
    boolean first = false;
    for (ElementDefinitionExampleComponent ex : examples) {
      if (first)
        first = false;
      else
        b.append("<br/>");
      b.append("<b>"+Utilities.escapeXml(ex.getLabel())+"</b>:"+encodeValue(ex.getValue())+"\r\n");
    }
    return b.toString();
    
  }
  private String encodeValue(Type value) throws Exception {
    if (value == null || value.isEmpty())
      return null;
    if (value instanceof PrimitiveType)
      return Utilities.escapeXml(((PrimitiveType) value).asStringValue());

    ByteArrayOutputStream bs = new ByteArrayOutputStream();
    XmlParser parser = new XmlParser();
    parser.setOutputStyle(OutputStyle.PRETTY);
    parser.compose(bs, null, value);
    String[] lines = bs.toString().split("\\r?\\n");
    StringBuilder b = new StringBuilder();
    for (String s : lines) {
      if (!Utilities.noString(s) && !s.startsWith("<?")) { // eliminate the xml header 
        b.append(Utilities.escapeXml(s).replace(" ", "&nbsp;")+"<br/>");
      }
    }
    return b.toString();  

  }

  private String getMapping(StructureDefinition profile, ElementDefinition d, String uri) {
    String id = null;
    for (StructureDefinitionMappingComponent m : profile.getMapping()) {
      if (m.hasUri() && m.getUri().equals(uri))
        id = m.getIdentity();
    }
    if (id == null)
      return null;
    for (ElementDefinitionMappingComponent m : d.getMapping()) {
      if (m.getIdentity().equals(id))
        return m.getMap();
    }
    return null;
  }


  private String describeAliases(List<StringType> synonym) {
    CommaSeparatedStringBuilder b = new CommaSeparatedStringBuilder();
    for (StringType s : synonym) 
      b.append(gt(s));
    return b.toString();
  }

  public String mappings(boolean complete) {
    if (sd.getMapping().isEmpty())
      return "<p>"+translate("sd.maps", "No Mappings")+"</p>";
    else {
      StringBuilder s = new StringBuilder();
      for (StructureDefinitionMappingComponent map : sd.getMapping()) {

        s.append("<a name=\""+map.getIdentity() +"\"> </a><h3>"+translate("sd.maps", "Mappings for %s (%s)", Utilities.escapeXml(gt(map.getNameElement())), map.getUri())+"</h3>");
        if (map.hasComment())
          s.append("<p>"+Utilities.escapeXml(gt(map.getCommentElement()))+"</p>");
//        else if (specmaps != null && preambles.has(map.getUri()))   
//          s.append(preambles.get(map.getUri()).getAsString());

        s.append("<table class=\"grid\">\r\n");

        s.append(" <tr><td colspan=\"3\"><b>"+Utilities.escapeXml(gt(sd.getNameElement()))+"</b></td></tr>\r\n");
        String path = null;
        for (ElementDefinition e : sd.getSnapshot().getElement()) {
          if (path == null || !e.getPath().startsWith(path)) {
            path = null;
            if (e.hasMax() && e.getMax().equals("0") || !(complete || hasMappings(e, map))) {
              path = e.getPath()+".";
            } else
              genElement(s, e, map.getIdentity());
          }
        }
        s.append("</table>\r\n");
      }
      return s.toString();
    }
  }

  private boolean hasMappings(ElementDefinition e, StructureDefinitionMappingComponent map) {
    ElementDefinitionMappingComponent m = getMap(e, map.getIdentity());
    if (m != null)
      return true;
    int i = sd.getSnapshot().getElement().indexOf(e)+1;
    while (i < sd.getSnapshot().getElement().size()) {
      ElementDefinition t =  sd.getSnapshot().getElement().get(i);
      if (t.getPath().startsWith(e.getPath()+".")) {
        m = getMap(t, map.getIdentity());
        if (m != null)
          return true;
      }
      i++;
    }
    return false;
  }

  private void genElement(StringBuilder s, ElementDefinition e, String id) {
    s.append(" <tr><td>");
    boolean root = true;
    for (char c : e.getPath().toCharArray()) 
      if (c == '.') {
        s.append("&nbsp;");
        s.append("&nbsp;");
        s.append("&nbsp;");
        root = false;
      }
    if (root)
      s.append(e.getPath());
    else
      s.append(tail(e.getPath()));
    if (e.hasSliceName()) {
      s.append(" (");
      s.append(Utilities.escapeXml(e.getSliceName()));
      s.append(")");
    }
    s.append("</td>");
    ElementDefinitionMappingComponent m = getMap(e, id);
    if (m == null)
      s.append("<td></td>");
    else
      s.append("<td>"+Utilities.escapeXml(m.getMap())+"</td>");
    s.append(" </tr>\r\n");
  }


  private ElementDefinitionMappingComponent getMap(ElementDefinition e, String id) {
    for (ElementDefinitionMappingComponent m : e.getMapping()) {
      if (m.getIdentity().equals(id))
        return m;
    }
    return null;
  }

  public String header() throws Exception {
    StringBuilder b = new StringBuilder();
    b.append("<p>\r\n");
    b.append(translate("sd.header", "The official URL for this profile is:")+"\r\n");
    b.append("</p>\r\n");
    b.append("<pre>"+sd.getUrl()+"</pre>\r\n");
    b.append("<p>\r\n");
    b.append(processMarkdown("description", sd.getDescriptionElement()));
    b.append("</p>\r\n");
    if (sd.getDerivation() == TypeDerivationRule.CONSTRAINT) {
      b.append("<p>\r\n");
      StructureDefinition sdb = context.fetchResource(StructureDefinition.class, sd.getBaseDefinition());
      if (sdb != null)
        b.append(translate("sd.header", "This profile builds on")+" <a href=\""+sdb.getUserString("path")+"\">"+gt(sdb.getNameElement())+"</a>.");
      else
        b.append(translate("sd.header", "This profile builds on")+" "+sd.getBaseDefinition()+".");
      b.append("</p>\r\n");
    }
    b.append("<p>\r\n");
    b.append(translate("sd.header", "This profile was published on %s as a %s by %s.\r\n", toStr(sd.getDate()), egt(sd.getStatusElement()), gt(sd.getPublisherElement())));
    b.append("</p>\r\n");
    return b.toString();
  }

  public String exampleList(List<FetchedFile> fileList) {
    StringBuilder b = new StringBuilder();
    for (FetchedFile f : fileList) {
      for (FetchedResource r : f.getResources()) {
        for (String p : r.getProfiles()) {
          if (sd.getUrl().equals(p)) {
            String name = r.getTitle();
            if (Utilities.noString(name))
              name = "example";
            String ref = igp.getLinkFor(r);
            b.append(" <li><a href=\""+ref+"\">"+Utilities.escapeXml(name)+"</a></li>\r\n");
          }
        }
      }
    }
    return b.toString();
  }

  public String span(boolean onlyConstraints, String canonical) throws IOException, FHIRException {
    return new XhtmlComposer().compose(utils.generateSpanningTable(sd, destDir, onlyConstraints, canonical));
  }

}
