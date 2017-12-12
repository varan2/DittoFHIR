package org.hl7.fhir.definitions.parsers;

/*
 Copyright (c) 2011+, HL7, Inc
 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification, 
 are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this 
 list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 this list of conditions and the following disclaimer in the documentation 
 and/or other materials provided with the distribution.
 * Neither the name of HL7 nor the names of its contributors may be used to 
 endorse or promote products derived from this software without specific 
 prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 POSSIBILITY OF SUCH DAMAGE.

 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.hl7.fhir.definitions.generators.specification.DataTypeTableGenerator;
import org.hl7.fhir.definitions.generators.specification.ProfileGenerator;
import org.hl7.fhir.definitions.generators.specification.ToolResourceUtilities;
import org.hl7.fhir.definitions.model.BindingSpecification;
import org.hl7.fhir.definitions.model.CommonSearchParameter;
import org.hl7.fhir.definitions.model.Compartment;
import org.hl7.fhir.definitions.model.ConstraintStructure;
import org.hl7.fhir.definitions.model.DefinedCode;
import org.hl7.fhir.definitions.model.DefinedStringPattern;
import org.hl7.fhir.definitions.model.Definitions;
import org.hl7.fhir.definitions.model.Dictionary;
import org.hl7.fhir.definitions.model.ElementDefn;
import org.hl7.fhir.definitions.model.EventDefn;
import org.hl7.fhir.definitions.model.Example;
import org.hl7.fhir.definitions.model.Example.ExampleType;
import org.hl7.fhir.definitions.model.ImplementationGuideDefn;
import org.hl7.fhir.definitions.model.Invariant;
import org.hl7.fhir.definitions.model.LogicalModel;
import org.hl7.fhir.definitions.model.PrimitiveType;
import org.hl7.fhir.definitions.model.Profile;
import org.hl7.fhir.definitions.model.Profile.ConformancePackageSourceType;
import org.hl7.fhir.definitions.model.ProfiledType;
import org.hl7.fhir.definitions.model.ResourceDefn;
import org.hl7.fhir.definitions.model.ResourceDefn.StandardsStatus;
import org.hl7.fhir.definitions.model.SearchParameterDefn;
import org.hl7.fhir.definitions.model.W5Entry;
import org.hl7.fhir.definitions.model.WorkGroup;
import org.hl7.fhir.definitions.validation.FHIRPathUsage;
import org.hl7.fhir.r4.conformance.ProfileUtilities;
import org.hl7.fhir.r4.formats.FormatUtilities;
import org.hl7.fhir.r4.formats.XmlParser;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.SearchParameter;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureDefinition.TypeDerivationRule;
import org.hl7.fhir.r4.utils.ToolingExtensions;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.exceptions.FHIRFormatError;
import org.hl7.fhir.igtools.spreadsheets.CodeSystemConvertor;
import org.hl7.fhir.igtools.spreadsheets.MappingSpace;
import org.hl7.fhir.igtools.spreadsheets.TypeParser;
import org.hl7.fhir.igtools.spreadsheets.TypeRef;
import org.hl7.fhir.tools.publisher.BuildWorkerContext;
import org.hl7.fhir.tools.publisher.PageProcessor;
import org.hl7.fhir.utilities.CSFile;
import org.hl7.fhir.utilities.CSFileInputStream;
import org.hl7.fhir.utilities.IniFile;
import org.hl7.fhir.utilities.Logger;
import org.hl7.fhir.utilities.Logger.LogMessageType;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.XLSXmlParser;
import org.hl7.fhir.utilities.XLSXmlParser.Sheet;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlComposer;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.hl7.fhir.utilities.xhtml.XhtmlParser;
import org.hl7.fhir.utilities.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This class parses the master source for FHIR into a single definitions object
 * model
 * 
 * The class knows where to find things, and what order to load them in
 * 
 * @author Grahame
 * 
 */
public class SourceParser {

  private final Logger logger;
  private final IniFile ini;
  private final Definitions definitions;
  private final String srcDir;
  private final String dstDir;
  private final String imgDir;
  private final String termDir;
  public String dtDir;
  private final String rootDir;
  private final OIDRegistry registry;
  private final String version;
  private final BuildWorkerContext context;
  private final Calendar genDate;
  private final Map<String, StructureDefinition> extensionDefinitions;
  private final PageProcessor page;
  private final Set<String> igNames = new HashSet<String>();
  private boolean forPublication;
  private List<FHIRPathUsage> fpUsages;
  

  public SourceParser(Logger logger, String root, Definitions definitions, boolean forPublication, String version, BuildWorkerContext context, Calendar genDate, Map<String, StructureDefinition> extensionDefinitions, PageProcessor page, List<FHIRPathUsage> fpUsages) throws IOException {
    this.logger = logger;
    this.forPublication = forPublication;
    this.registry = new OIDRegistry(root, forPublication);
    this.definitions = definitions;
    this.version = version;
    this.context = context;
    this.genDate = genDate;
    this.page = page;
    this.fpUsages = fpUsages;

    char sl = File.separatorChar;
    srcDir = root + sl + "source" + sl;
    dstDir = root + sl + "publish" + sl;
    ini = new IniFile(srcDir + "fhir.ini");

    termDir = srcDir + "terminologies" + sl;
    dtDir = srcDir + "datatypes" + sl;
    imgDir = root + sl + "images" + sl;
    rootDir = root + sl;
    this.extensionDefinitions = extensionDefinitions;
    vsGen = new ValueSetGenerator(definitions, version, genDate);
  }




  public void parse(Calendar genDate, List<ValidationMessage> issues) throws Exception {
    logger.log("Loading", LogMessageType.Process);

    loadWorkGroups();
    loadW5s();
    loadMappingSpaces();
    loadGlobalBindings();

    loadTLAs();
    loadIgs();
    loadTypePages();
    loadDictionaries();
    loadStyleExemptions();

    loadCommonSearchParameters();
    loadPrimitives();

    for (String id : ini.getPropertyNames("search-rules"))
      definitions.seachRule(id, ini.getStringProperty("search-rules", id));
    
    for (String id : ini.getPropertyNames("valueset-fixup"))
      definitions.getVsFixups().add(id);

    for (String n : ini.getPropertyNames("removed-resources"))
      definitions.getDeletedResources().add(n);

    for (String n : ini.getPropertyNames("infrastructure"))
      loadCompositeType(n, definitions.getInfrastructure());

    for (String n : ini.getPropertyNames("types"))
      loadCompositeType(n, definitions.getTypes());	
    for (String n : ini.getPropertyNames("structures"))
      loadCompositeType(n, definitions.getStructures());

    String[] shared = ini.getPropertyNames("shared"); 
    if(shared != null)
      for (String n : shared )
        definitions.getShared().add(loadCompositeType(n, definitions.getStructures()));

    String[] logical = ini.getPropertyNames("logical"); 
    if(logical != null)
      for (String n : logical)
        definitions.getIgs().get("core").getLogicalModels().add(loadLogicalModel(n));


    // basic infrastructure
    for (String n : ini.getPropertyNames("resource-infrastructure")) {
      ResourceDefn r = loadResource(n, null, true, false);
      String[] parts = ini.getStringProperty("resource-infrastructure", n).split("\\,");
      if (parts[0].equals("abstract"))
        r.setAbstract(true);
      definitions.getBaseResources().put(parts[1], r);
    }

    logger.log("Load Resource Templates", LogMessageType.Process);
    for (String n : ini.getPropertyNames("resource-templates"))
      loadResource(n, definitions.getResourceTemplates(), false, true);
    
    logger.log("Load Resources", LogMessageType.Process);
    for (String n : ini.getPropertyNames("resources"))
      loadResource(n, definitions.getResources(), false, false);

    processSearchExpressions();
    processContainerExamples();
    logger.log("Load Extras", LogMessageType.Process);
    loadCompartments();
    loadStatusCodes();
    buildSpecialValues();

    for (String n : ini.getPropertyNames("svg"))
      definitions.getDiagrams().put(n, ini.getStringProperty("svg", n));

    for (String n : ini.getPropertyNames("special-resources"))
      definitions.getAggregationEndpoints().add(n);

    logger.log("Load Code Systems", LogMessageType.Process);
    String[] pn = ini.getPropertyNames("codesystems");
    if (pn != null)
      for (String n : pn) {
        loadCodeSystem(n);
      }
    logger.log("Load Value Sets", LogMessageType.Process);
    pn = ini.getPropertyNames("valuesets");
    if (pn != null)
      for (String n : pn) {
        loadValueSet(n);
      }
    logger.log("Load Profiles", LogMessageType.Process);
    for (String n : ini.getPropertyNames("profiles")) { // todo-profile: rename this
      loadConformancePackages(n, issues);
    }

    for (ResourceDefn r : definitions.getBaseResources().values()) {
      for (Profile p : r.getConformancePackages()) 
        loadConformancePackage(p, issues, r.getWg());
    }
    for (ResourceDefn r : definitions.getResources().values()) {
      for (Profile p : r.getConformancePackages()) 
        loadConformancePackage(p, issues, r.getWg());
    }
    definitions.setLoaded(true);
    
    for (ImplementationGuideDefn ig : definitions.getSortedIgs()) {
      if (!Utilities.noString(ig.getSource())) {
        try {
          new IgParser(page, page.getWorkerContext(), page.getGenDate(), page, definitions.getCommonBindings(), wg(ig.getCommittee()), definitions.getMapTypes(), definitions.getProfileIds(), definitions.getCodeSystems(), registry, page.getConceptMaps(), definitions.getWorkgroups()).load(rootDir, ig, issues, igNames);
          // register what needs registering
          for (ValueSet vs : ig.getValueSets()) {
            definitions.getExtraValuesets().put(vs.getId(), vs);
            context.getValueSets().put(vs.getUrl(), vs);
          }
          for (Example ex : ig.getExamples())
            definitions.getResourceByName(ex.getResourceName()).getExamples().add(ex);
          for (Profile p : ig.getProfiles()) {
            if (definitions.getPackMap().containsKey(p.getId()))
              throw new Exception("Duplicate Pack id "+p.getId());
            definitions.getPackList().add(p);
            definitions.getPackMap().put(p.getId(), p);
          }
          for (Dictionary d : ig.getDictionaries())
            definitions.getDictionaries().put(d.getId(), d);
        } catch (Exception e) {
          throw new Exception("Error reading IG "+ig.getSource()+": "+e.getMessage(), e);
        }
      }        
    }
    closeTemplates();
  }

  private WorkGroup wg(String committee) {
    return definitions.getWorkgroups().get(committee);
  }




  private void loadCommonSearchParameters() throws FHIRFormatError, FileNotFoundException, IOException {
    Bundle bnd = (Bundle) new XmlParser().parse(new CSFileInputStream(Utilities.path(srcDir, "searchparameter", "common-search-parameters.xml")));
    for (BundleEntryComponent be : bnd.getEntry()) {
      SearchParameter sp = (SearchParameter) be.getResource();
      CommonSearchParameter csp = new CommonSearchParameter();
      csp.setId(sp.getId());
      csp.setCode(sp.getCode());
      for (CodeType ct : sp.getBase()) {
        csp.getResources().add(ct.asStringValue());
        definitions.getCommonSearchParameters().put(ct.asStringValue()+"::"+sp.getCode(), csp);
      }
    }
  }

  private void closeTemplates() throws Exception {
    for (ResourceDefn r : definitions.getResourceTemplates().values()) 
      closeTemplate(r.getRoot(), Utilities.unCamelCase(r.getName()), 0);
  }

  private void closeTemplate(ElementDefn element, String title, int level) throws Exception {
    if (element.getShortDefn() != null)
      element.setShortDefn(element.getShortDefn().replace("{{title}}", title));
    if (element.getDefinition() != null)
      element.setDefinition(element.getDefinition().replace("{{title}}", title));
    if (element.getComments() != null)
      element.setComments(element.getComments().replace("{{title}}", title));
    if (element.getCommitteeNotes() != null)
      element.setCommitteeNotes(element.getCommitteeNotes().replace("{{title}}", title));
    if (element.getRequirements() != null)
      element.setRequirements(element.getRequirements().replace("{{title}}", title));
    if (element.getTodo() != null)
      element.setTodo(element.getTodo().replace("{{title}}", title));
    if (level == 0) {
      // we're preparing this for code generation now; we want to ditch any labelled as 'no-gen'
      List<ElementDefn> delete = new ArrayList<ElementDefn>();
      for (ElementDefn child : element.getElements()) 
        if (wantToGenerate(child))
          closeTemplate(child, title, level+1);
        else
          delete.add(child);
      element.getElements().removeAll(delete);   
    } else 
      if (!element.getElements().isEmpty()) {
        throw new Exception("No complex elements in a template - found "+element.getName()+"."+element.getElements().get(0).getName()); 
      }
  }


  private boolean wantToGenerate(ElementDefn child) {
    String gen = child.getMapping("http://hl7.org/fhir/object-implementation");
    if (gen == null)
      return true;
    else
      return !"no-gen-base".equals(gen);
  }




  private LogicalModel loadLogicalModel(String n) throws Exception {
    File spreadsheet = new CSFile(Utilities.path(srcDir, n, n+"-spreadsheet.xml"));    
    SpreadsheetParser sparser = new SpreadsheetParser(n, new CSFileInputStream(spreadsheet), spreadsheet.getName(), definitions, srcDir, logger, registry, version, context, genDate, false, extensionDefinitions, page, false, ini, wg("fhir"), definitions.getProfileIds(), fpUsages, page.getConceptMaps());
    sparser.setFolder(Utilities.getDirectoryForFile(spreadsheet.getAbsolutePath()));
    LogicalModel lm = sparser.parseLogicalModel();
    lm.setId(n);
    lm.setSource(spreadsheet.getAbsolutePath());
    lm.getResource().setName(lm.getId());
    return lm;
  }




  private void processSearchExpressions() throws Exception {
    for (ResourceDefn rd : definitions.getBaseResources().values())
      processSearchExpressions(rd);      
    for (ResourceDefn rd : definitions.getResources().values())
      processSearchExpressions(rd);          
  }

  private void processSearchExpressions(ResourceDefn rd) throws Exception {
    for (SearchParameterDefn sp : rd.getSearchParams().values())
      if (Utilities.noString(sp.getExpression()))
        sp.setExpression(convertToExpression(rd, sp.getPaths()));    
  }

  private String convertToExpression(ResourceDefn rd, List<String> pn) throws Exception {
    StringBuilder b = new StringBuilder();
    boolean first = true;
    for (String p : pn) {
      ElementDefn ed;
      if (p.startsWith(rd.getName()+"."))
        ed = rd.getRoot().getElementByName(p, true, definitions, "search parameter generation", true);
      else
        throw new Exception("huh?");
      if (ed == null)
        throw new Exception("not found: "+p);
      
      if (ed.getName().endsWith("[x]"))
        if (p.endsWith("[x]"))
          p = p.substring(0, p.length()-3);
        else {
          int lp = p.lastIndexOf(".")+ed.getName().length()-2;
          p = p.substring(0, lp)+".as("+p.substring(lp)+")";
        }
      if (first)
        first = false;
      else
        b.append(" | ");
      b.append(p);
    }
    return b.toString();
  }



  private void processContainerExamples() throws Exception {
    for (ResourceDefn defn : definitions.getResources().values()) 
      for (Example ex : defn.getExamples()) {
        if (ex.getType() == ExampleType.Container) {
          processContainerExample(ex);
        }
      }
  }
    

  private void processContainerExample(Example ex) throws Exception {
      Map<String, Document> parts = divideContainedResources(ex.getId(), ex.getXml());
      for (String n : parts.keySet()) {
        definitions.getResourceByName(parts.get(n).getDocumentElement().getNodeName()).getExamples().add(new Example("Part of "+ex.getName(), ex.getId()+"-"+n, ex.getTitle()+"-"+n, "Part of the example", false, ExampleType.XmlFile, parts.get(n)));
      }
  }

  private Map<String, Document> divideContainedResources(String rootId, Document doc) throws Exception {
    Map<String, Document> res = new HashMap<String, Document>();
    List<Element> list = new ArrayList<Element>();
    XMLUtil.getNamedChildren(doc.getDocumentElement(), "contained", list);
    for (Element e : list) {
      Element r = XMLUtil.getFirstChild(e);
      String id = XMLUtil.getNamedChildValue(r, "id");
      if (Utilities.noString(id))
        throw new Exception("Contained Resource has no id");
      String nid = rootId + "-"+id;
      if (!nid.matches(FormatUtilities.ID_REGEX))
        throw new Exception("Contained Resource combination is illegal");
      replaceReferences(doc.getDocumentElement(), id, r.getNodeName()+"/"+nid);
      XMLUtil.setNamedChildValue(r, "id", nid);
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document ndoc = docBuilder.newDocument();
      Node newNode = ndoc.importNode(r, true);
      ndoc.appendChild(newNode);
      res.put(id, ndoc);
      doc.getDocumentElement().removeChild(e);
    }
    return res;
  }


  private void replaceReferences(Element e, String id, String nid) throws Exception {
    if (("#"+id).equals(XMLUtil.getNamedChildValue(e, "reference")))
      XMLUtil.setNamedChildValue(e, "reference", nid);
    Element c = XMLUtil.getFirstChild(e);
    while (c != null) {
      replaceReferences(c, id, nid);
      c = XMLUtil.getNextSibling(c);
    }   
  }

  private void loadStyleExemptions() {
    for (String s : ini.getPropertyNames("style-exempt"))
      definitions.getStyleExemptions().add(s);
  }


  private void buildSpecialValues() throws Exception {
    for (ValueSet vs : definitions.getBoundValueSets().values())
      vsGen.check(vs);
  }


  private void loadDictionaries() throws IOException {
    String[] dicts = ini.getPropertyNames("dictionaries");
    if (dicts != null) {
      for (String dict : dicts) {
        String[] s = ini.getStringProperty("dictionaries", dict).split("\\:");
        definitions.getDictionaries().put(dict, new Dictionary(dict, s[1], s[0], Utilities.path(page.getFolders().srcDir, "dictionaries", dict+".xml"), null));
      }
    }
  }


  private void loadIgs() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true); 
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document xdoc = builder.parse(new CSFileInputStream(srcDir + "igs.xml"));
    Element root = xdoc.getDocumentElement();
    if (root.getNodeName().equals("igs")) {
      Element ig = XMLUtil.getFirstChild(root);
      while (ig != null) {
        if (ig.getNodeName().equals("ig") && (!ig.hasAttribute("local") || isOkLocally(ig.getAttribute("code"))) && !isRuledOutLocally(ig.getAttribute("code"))) {
          ImplementationGuideDefn igg = new ImplementationGuideDefn(ig.getAttribute("committee"), ig.getAttribute("code"), ig.getAttribute("name"), ig.getAttribute("brief"), 
              ig.getAttribute("source").replace('\\', File.separatorChar), "1".equals(ig.getAttribute("review")),
              ig.getAttribute("ballot"), ig.getAttribute("fmm"), ig.getAttribute("section"), "yes".equals(ig.getAttribute("core")), page.getValidationErrors());
          definitions.getIgs().put(igg.getCode(), igg);
          definitions.getSortedIgs().add(igg);
        }
        ig = XMLUtil.getNextSibling(ig);
      }
    }
  }

  private boolean isRuledOutLocally(String code) throws IOException {
    String inifile = Utilities.path(rootDir, "local.ini");
    if (!new File(inifile).exists())
      return false;
    IniFile ini = new IniFile(inifile);
    boolean ok = "false".equals(ini.getStringProperty("igs", code));
    return ok;
  }

  private boolean isOkLocally(String code) throws IOException {
    if (forPublication)
      return false;
    String inifile = Utilities.path(rootDir, "local.ini");
    if (!new File(inifile).exists())
      return false;
    IniFile ini = new IniFile(inifile);
    boolean ok = "true".equals(ini.getStringProperty("igs", code));
    return ok;
  }

  private void loadTypePages() {
    String[] tps = ini.getPropertyNames("type-pages");
    for (String tp : tps) {
      String s = ini.getStringProperty("type-pages", tp);
      definitions.getTypePages().put(tp, s);
    }        
  }

  private void loadW5s() throws IOException {
    if (new File(Utilities.path(srcDir, "w5.ini")).exists()) {
      IniFile w5 = new IniFile(Utilities.path(srcDir, "w5.ini"));
      for (String n : w5.getPropertyNames("names")) {
        W5Entry w5o = new W5Entry(n, w5.getStringProperty("names", n), w5.getBooleanProperty("display", n), w5.getStringProperty("subclasses", n));
        definitions.getW5list().add(w5o);
        definitions.getW5s().put(n, w5o);
      }
    }
  }


  private void loadTLAs() throws Exception {
    Set<String> tlas = new HashSet<String>();

    if (ini.getPropertyNames("tla") != null) {
      for (String n : ini.getPropertyNames("tla")) {
        String tla = ini.getStringProperty("tla", n);
        if (tlas.contains(tla))
          throw new Exception("Duplicate TLA "+tla+" for "+n);
        tlas.add(tla);
        definitions.getTLAs().put(n.toLowerCase(), tla);        
      }
    }

  }


  private void loadWorkGroups() {
    String[] wgs = ini.getPropertyNames("wg-info");
    for (String wg : wgs) {
      String s = ini.getStringProperty("wg-info", wg);
      int i = s.indexOf(" ");
      String url = s.substring(0, i);
      String name = s.substring(i+1).trim();
      definitions.getWorkgroups().put(wg, new WorkGroup(wg, name, url));
    }    
  }


  private void loadMappingSpaces() throws Exception {
    FileInputStream is = null;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      is = new FileInputStream(Utilities.path(srcDir, "mappingSpaces.xml"));
      Document doc = builder.parse(is);
      Element e = XMLUtil.getFirstChild(doc.getDocumentElement());
      while (e != null) {
        MappingSpace m = new MappingSpace(XMLUtil.getNamedChild(e, "columnName").getTextContent(), XMLUtil.getNamedChild(e, "title").getTextContent(), 
            XMLUtil.getNamedChild(e, "id").getTextContent(), Integer.parseInt(XMLUtil.getNamedChild(e, "sort").getTextContent()), isTrue(XMLUtil.getNamedChild(e, "publish")));
        definitions.getMapTypes().put(XMLUtil.getNamedChild(e, "url").getTextContent(), m);
        Element p = XMLUtil.getNamedChild(e, "preamble");
        if (p != null)
          m.setPreamble(new XhtmlParser().parseHtmlNode(p).setName("div"));
        e = XMLUtil.getNextSibling(e);
      }
    } catch (Exception e) {
      throw new Exception("Error processing mappingSpaces.xml: "+e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  private boolean isTrue(Element c) {
    return c != null &&  "true".equals(c.getTextContent());
  }

  private void loadStatusCodes() throws FileNotFoundException, Exception {
    XLSXmlParser xml = new XLSXmlParser(new CSFileInputStream(srcDir+"status-codes.xml"), "Status Codes");
    Sheet sheet = xml.getSheets().get("Status Codes");
    for (int row = 0; row < sheet.rows.size(); row++) {
      String path = sheet.getColumn(row, "Path");
      ArrayList<String> codes = new ArrayList<String>();
      for (int i = 1; i <= 80; i++) {
        String s = sheet.getColumn(row, "c"+Integer.toString(i));
        if (s.endsWith("?"))
          s = s.substring(0, s.length()-1);
        codes.add(s);
      }
      definitions.getStatusCodes().put(path, codes); 
    }    
  }

  private void loadCompartments() throws FileNotFoundException, Exception {
    XLSXmlParser xml = new XLSXmlParser(new CSFileInputStream(srcDir+"compartments.xml"), "compartments.xml");
    Sheet sheet = xml.getSheets().get("compartments");
    for (int row = 0; row < sheet.rows.size(); row++) {
      Compartment c = new Compartment();
      c.setName(sheet.getColumn(row, "Name"));
      if (!c.getName().startsWith("!")) {
        c.setTitle(sheet.getColumn(row, "Title"));
        c.setDescription(sheet.getColumn(row, "Description"));
        c.setIdentity(sheet.getColumn(row, "Identification"));
        c.setMembership(sheet.getColumn(row, "Inclusion"));

        definitions.getCompartments().add(c);
      }
    }
    sheet = xml.getSheets().get("resources");
    for (int row = 0; row < sheet.rows.size(); row++) {
      String mn = sheet.getColumn(row, "Resource");
      if (!mn.startsWith("!")) {
        ResourceDefn r = definitions.getResourceByName(mn);
        for (Compartment c : definitions.getCompartments()) {
          c.getResources().put(r,  sheet.getColumn(row, c.getName()));
        }
      }
    }    
  }

  private void loadCodeSystem(String n) throws FileNotFoundException, Exception {
    XmlParser xml = new XmlParser();
    CodeSystem cs = (CodeSystem) xml.parse(new CSFileInputStream(srcDir+ini.getStringProperty("codesystems", n).replace('\\', File.separatorChar)));
    if (!cs.hasId())  
      cs.setId(FormatUtilities.makeId(n));
    cs.setUserData("path", "codesystem-"+cs.getId()+".html");
    cs.setUserData("filename", "codesystem-"+cs.getId());
    definitions.getCodeSystems().put(cs.getUrl(), cs);
  }


  private void loadValueSet(String n) throws FileNotFoundException, Exception {
    XmlParser xml = new XmlParser();
    ValueSet vs = (ValueSet) xml.parse(new CSFileInputStream(srcDir+ini.getStringProperty("valuesets", n).replace('\\', File.separatorChar)));
    new CodeSystemConvertor(definitions.getCodeSystems()).convert(xml, vs, srcDir+ini.getStringProperty("valuesets", n).replace('\\', File.separatorChar));
    vs.setId(FormatUtilities.makeId(n));
    vs.setUrl("http://hl7.org/fhir/ValueSet/"+vs.getId());
    vs.setUserData("path", "valueset-"+vs.getId()+".html");
    vs.setUserData("filename", "valueset-"+vs.getId());
    definitions.getExtraValuesets().put(n, vs);
    definitions.getExtraValuesets().put(vs.getUrl(), vs);
  }

  private void loadConformancePackages(String n, List<ValidationMessage> issues) throws Exception {
    String usage = "core";
    String[] v = ini.getStringProperty("profiles", n).split("\\:");
    File spreadsheet = new CSFile(rootDir+v[1]);
    if (TextFile.fileToString(spreadsheet.getAbsolutePath()).contains("urn:schemas-microsoft-com:office:spreadsheet")) {
      SpreadsheetParser sparser = new SpreadsheetParser(n, new CSFileInputStream(spreadsheet), spreadsheet.getName(), definitions, srcDir, logger, registry, version, context, genDate, false, extensionDefinitions, page, false, ini, wg(v[0]), definitions.getProfileIds(), fpUsages, page.getConceptMaps());
      try {
        Profile pack = new Profile(usage);
        pack.setTitle(n);
        pack.setSource(spreadsheet.getAbsolutePath());
        pack.setSourceType(ConformancePackageSourceType.Spreadsheet);
        if (definitions.getPackMap().containsKey(n))
          throw new Exception("Duplicate Pack id "+n);
        definitions.getPackList().add(pack);
        definitions.getPackMap().put(n, pack);
        sparser.parseConformancePackage(pack, definitions, Utilities.getDirectoryForFile(spreadsheet.getAbsolutePath()), pack.getCategory(), issues, null);
      } catch (Exception e) {
        throw new Exception("Error Parsing StructureDefinition: '"+n+"': "+e.getMessage(), e);
      }
    } else {
      Profile pack = new Profile(usage);
      parseConformanceDocument(pack, n, spreadsheet, usage, null);
      if (definitions.getPackMap().containsKey(n))
        throw new Exception("Duplicate Pack id "+n);
      definitions.getPackList().add(pack);
      definitions.getPackMap().put(n, pack);
      throw new Error("check this!");
    }
  }


  private void parseConformanceDocument(Profile pack, String n, File file, String usage, WorkGroup wg) throws Exception {
    try {
      Resource rf = new XmlParser().parse(new CSFileInputStream(file));
      if (!(rf instanceof Bundle))
        throw new Exception("Error parsing Profile: neither a spreadsheet nor a bundle");
      Bundle b = (Bundle) rf;
      if (b.getType() != BundleType.DOCUMENT)
        throw new Exception("Error parsing profile: neither a spreadsheet nor a bundle that is a document");
      for (BundleEntryComponent ae : ((Bundle) rf).getEntry()) {
        if (ae.getResource() instanceof Composition)
          pack.loadFromComposition((Composition) ae.getResource(), file.getAbsolutePath());
        else if (ae.getResource() instanceof StructureDefinition && !((StructureDefinition) ae.getResource()).getType().equals("Extension")) {
          StructureDefinition ed = (StructureDefinition) ae.getResource();
          for (StringType s : ed.getContext())
            definitions.checkContextValid(ed.getContextType(), s.getValue(), file.getName());
          ToolResourceUtilities.updateUsage(ed, pack.getCategory());
          pack.getProfiles().add(new ConstraintStructure(ed, definitions.getUsageIG(usage, "Parsing "+file.getAbsolutePath()), wg == null ? wg(ed) : wg, fmm(ed), ed.getExperimental()));
        } else if (ae.getResource() instanceof StructureDefinition) {
          StructureDefinition ed = (StructureDefinition) ae.getResource();
          if (Utilities.noString(ed.getBaseDefinition()))
            ed.setBaseDefinition("http://hl7.org/fhir/StructureDefinition/Extension");
          ed.setDerivation(TypeDerivationRule.CONSTRAINT);
          if (ToolingExtensions.readStringExtension(ed, ToolingExtensions.EXT_WORKGROUP) == null)
            ToolingExtensions.setCodeExtension(ed, ToolingExtensions.EXT_WORKGROUP, wg.getCode());
          context.seeExtensionDefinition(ae.hasFullUrl() ? ae.getFullUrl() : "http://hl7.org/fhir/StructureDefinition/"+ed.getId(), ed);
          pack.getExtensions().add(ed);
        }
      }
    } catch (Exception e) {
      throw new Exception("Error Parsing profile: '"+n+"': "+e.getMessage(), e);
    }
  }


  private String fmm(StructureDefinition ed) {
    return Integer.toString(ToolingExtensions.readIntegerExtension(ed, ToolingExtensions.EXT_FMM_LEVEL, 1)); // default fmm level
  }




  private WorkGroup wg(StructureDefinition ed) {
    return definitions.getWorkgroups().get(ToolingExtensions.readStringExtension(ed, ToolingExtensions.EXT_WORKGROUP));
  }


  private void loadConformancePackage(Profile ap, List<ValidationMessage> issues, WorkGroup wg) throws FileNotFoundException, IOException, Exception {
    if (ap.getSourceType() == ConformancePackageSourceType.Spreadsheet) {
      SpreadsheetParser sparser = new SpreadsheetParser(ap.getCategory(), new CSFileInputStream(ap.getSource()), Utilities.noString(ap.getId()) ? ap.getSource() : ap.getId(), definitions, srcDir, logger, registry, version, context, genDate, false, extensionDefinitions, page, false, ini, wg, definitions.getProfileIds(), fpUsages, page.getConceptMaps());
      sparser.setFolder(Utilities.getDirectoryForFile(ap.getSource()));
      sparser.parseConformancePackage(ap, definitions, Utilities.getDirectoryForFile(ap.getSource()), ap.getCategory(), issues, wg);
    } else if (ap.getSourceType() == ConformancePackageSourceType.StructureDefinition) {
      Resource rf = new XmlParser().parse(new CSFileInputStream(ap.getSource()));
      if (!(rf instanceof StructureDefinition)) 
        throw new Exception("Error parsing Profile: not a structure definition");
      StructureDefinition sd = (StructureDefinition) rf;
      ap.putMetadata("id", sd.getId()+"-pack");
      ap.putMetadata("date", sd.getDateElement().asStringValue());
      ap.putMetadata("title", sd.getTitle());
      ap.putMetadata("status", sd.getStatus().toCode());
      ap.putMetadata("description", new XhtmlComposer().compose(sd.getText().getDiv()));
      ap.setTitle(sd.getTitle());
      new ProfileUtilities(page.getWorkerContext(), null, null).setIds(sd, false);
      ap.getProfiles().add(new ConstraintStructure(sd, definitions.getUsageIG(ap.getCategory(), "Parsing "+ap.getSource()), wg == null ? wg(sd) : wg, fmm(sd), sd.getExperimental()));
    } else // if (ap.getSourceType() == ConformancePackageSourceType.Bundle) {
      parseConformanceDocument(ap, ap.getId(), new CSFile(ap.getSource()), ap.getCategory(), wg);
  }

  private void loadGlobalBindings() throws Exception {
    logger.log("Load Common Bindings", LogMessageType.Process);

    BindingsParser parser = new BindingsParser(new CSFileInputStream(new CSFile(termDir + "bindings.xml")), termDir + "bindings.xml", srcDir, registry, version, 
        definitions.getCodeSystems(), page.getConceptMaps(), genDate);
    List<BindingSpecification> cds = parser.parse();

    for (BindingSpecification cd : cds) {
      definitions.getAllBindings().add(cd);
      definitions.getCommonBindings().put(cd.getName(), cd);
      if (cd.getValueSet() != null) {
        vsGen.updateHeader(cd, cd.getValueSet());
        definitions.getBoundValueSets().put(cd.getValueSet().getUrl(), cd.getValueSet());
      } else if (cd.getReference() != null && cd.getReference().startsWith("http:")) {
        definitions.getUnresolvedBindings().add(cd);
      }
      if (cd.getMaxValueSet() != null) {
        vsGen.updateHeader(cd, cd.getMaxValueSet());
        definitions.getBoundValueSets().put(cd.getMaxValueSet().getUrl(), cd.getMaxValueSet());
      }
    }
    if (!page.getDefinitions().getBoundValueSets().containsKey("http://hl7.org/fhir/ValueSet/data-absent-reason"))
      throw new Exception("d-a-r not found");

  }

  private void loadPrimitives() throws Exception {
    XLSXmlParser xls = new XLSXmlParser(new CSFileInputStream(dtDir
        + "primitives.xml"), "primitives");
    Sheet sheet = xls.getSheets().get("Imports");
    for (int row = 0; row < sheet.rows.size(); row++) {
      processImport(sheet, row);
    }
    sheet = xls.getSheets().get("Patterns");
    for (int row = 0; row < sheet.rows.size(); row++) {
      processStringPattern(sheet, row);
    }
  }

  private void processImport(Sheet sheet, int row) throws Exception {
    PrimitiveType prim = new PrimitiveType();
    prim.setCode(sheet.getColumn(row, "Data Type"));
    prim.setDefinition(sheet.getColumn(row, "Definition"));
    prim.setComment(sheet.getColumn(row, "Comments"));
    prim.setSchemaType(sheet.getColumn(row, "Schema"));
    prim.setJsonType(sheet.getColumn(row, "Json"));
    prim.setRegex(sheet.getColumn(row, "RegEx"));
    prim.setV2(sheet.getColumn(row, "v2"));
    prim.setV3(sheet.getColumn(row, "v3"));
    TypeRef td = new TypeRef();
    td.setName(prim.getCode());
    definitions.getKnownTypes().add(td);
    definitions.getPrimitives().put(prim.getCode(), prim);
  }

  private void processStringPattern(Sheet sheet, int row) throws Exception {
    DefinedStringPattern prim = new DefinedStringPattern();
    prim.setCode(sheet.getColumn(row, "Data Type"));
    prim.setDefinition(sheet.getColumn(row, "Definition"));
    prim.setComment(sheet.getColumn(row, "Comments"));
    prim.setRegex(sheet.getColumn(row, "RegEx"));
    prim.setSchema(sheet.getColumn(row, "Schema"));
    prim.setJsonType(sheet.getColumn(row, "Json"));
    prim.setBase(sheet.getColumn(row, "Base"));
    TypeRef td = new TypeRef();
    td.setName(prim.getCode());
    definitions.getKnownTypes().add(td);
    definitions.getPrimitives().put(prim.getCode(), prim);
  }

  private void genTypeProfile(org.hl7.fhir.definitions.model.TypeDefn t) throws Exception {
    StructureDefinition profile;
    try {
      profile = new ProfileGenerator(definitions, context, page, genDate, version, null, fpUsages).generate(t);
      t.setProfile(profile);
      DataTypeTableGenerator dtg = new DataTypeTableGenerator(dstDir, page, t.getName(), true);
      t.getProfile().getText().setDiv(new XhtmlNode(NodeType.Element, "div"));
      t.getProfile().getText().getDiv().getChildNodes().add(dtg.generate(t));
      if (context.getProfiles().containsKey(t.getProfile().getUrl()))
        throw new Exception("Duplicate Profile "+t.getProfile().getUrl());
      context.getProfiles().put(t.getProfile().getUrl(), t.getProfile());
      context.getProfiles().put(t.getProfile().getName(), t.getProfile());
    } catch (Exception e) {
      throw new Exception("Error generating profile for '"+t.getName()+"': "+e.getMessage(), e);
    }
  }

  private String loadCompositeType(String n, Map<String, org.hl7.fhir.definitions.model.TypeDefn> map) throws Exception {
    TypeParser tp = new TypeParser();
    List<TypeRef> ts = tp.parse(n, false, null, context, true);
    definitions.getKnownTypes().addAll(ts);

    try {
      TypeRef t = ts.get(0);
      File csv = new CSFile(dtDir + t.getName().toLowerCase() + ".xml");
      if (csv.exists()) {
        SpreadsheetParser p = new SpreadsheetParser("core", new CSFileInputStream(csv), csv.getName(), definitions, srcDir, logger, registry, version, context, genDate, false, extensionDefinitions, page, true, ini, wg("fhir"), definitions.getProfileIds(), fpUsages, page.getConceptMaps());
        org.hl7.fhir.definitions.model.TypeDefn el = p.parseCompositeType();
        map.put(t.getName(), el);
        genTypeProfile(el);
        return el.getName();
      } else {
        String p = ini.getStringProperty("types", n);
        csv = new CSFile(dtDir + p.toLowerCase() + ".xml");
        if (!csv.exists())
          throw new Exception("unable to find a definition for " + n + " in " + p);
        XLSXmlParser xls = new XLSXmlParser(new CSFileInputStream(csv),
            csv.getAbsolutePath());
        Sheet sheet = xls.getSheets().get("Restrictions");
        boolean found = false;
        for (int i = 0; i < sheet.rows.size(); i++) {
          if (sheet.getColumn(i, "Name").equals(n)) {
            found = true;
            Invariant inv = new Invariant();
            inv.setId(n);
            inv.setEnglish(sheet.getColumn(i,"Rules"));
            inv.setOcl(sheet.getColumn(i, "OCL"));
            inv.setXpath(sheet.getColumn(i, "XPath"));
            inv.setExpression(sheet.getColumn(i, "Expression"));
            inv.setTurtle(sheet.getColumn(i, "RDF"));
            ProfiledType pt = new ProfiledType();
            pt.setDefinition(sheet.getColumn(i, "Definition"));
            pt.setDescription(sheet.getColumn(i, "Rules"));
            String structure = sheet.getColumn(i, "Structure");
            if (!Utilities.noString(structure)) {
              String[] parts = structure.split("\\;");
              for (String pp : parts) {
                String[] words = pp.split("\\=");
                pt.getRules().put(words[0], words[1]);
              }
            }
            pt.setName(n);
            pt.setBaseType(p);
            pt.setInvariant(inv);
            definitions.getConstraints().put(n, pt);
          }
        }
        if (!found)
          throw new Exception("Unable to find definition for " + n);
        return n;
      }
    } catch (Exception e) {
      throw new Exception("Unable to load "+n+": "+e.getMessage(), e);
    }
  }

  private ResourceDefn loadResource(String n, Map<String, ResourceDefn> map, boolean isAbstract, boolean isTemplate) throws Exception {
    String folder = n;
    File spreadsheet = new CSFile((srcDir) + folder + File.separatorChar + n + "-spreadsheet.xml");

    WorkGroup wg = definitions.getWorkgroups().get(ini.getStringProperty("workgroups", n));
    
    if (wg == null)
      throw new Exception("No Workgroup found for resource "+n+": '"+ini.getStringProperty("workgroups", n)+"'");
    
    SpreadsheetParser sparser = new SpreadsheetParser("core", new CSFileInputStream(
        spreadsheet), spreadsheet.getName(), definitions, srcDir, logger, registry, version, context, genDate, isAbstract, extensionDefinitions, page, false, ini, wg, definitions.getProfileIds(), fpUsages, page.getConceptMaps());
    ResourceDefn root;
    try {
      root = sparser.parseResource(isTemplate);
    } catch (Exception e) {
      throw new Exception("Error Parsing Resource "+n+": "+e.getMessage(), e);
    }
    root.setWg(wg);
    root.setFmmLevel(ini.getStringProperty("fmm", n.toLowerCase()));

    for (EventDefn e : sparser.getEvents())
      processEvent(e, root.getRoot());

    if (ini.getBooleanProperty("draft-resources", root.getName()))
      root.setStatus(StandardsStatus.DRAFT);
    else if (ini.getBooleanProperty("normative-resources", root.getName()))
      root.setStatus(StandardsStatus.NORMATIVE);
    
    if (map != null) {
      map.put(root.getName(), root);
    }
    if (!isTemplate) {
      definitions.getKnownResources().put(root.getName(), new DefinedCode(root.getName(), root.getRoot().getDefinition(), n));
      context.getResourceNames().add(root.getName());
    }
    return root;
  }

  private void processEvent(EventDefn defn, ElementDefn root)
      throws Exception {
    // first, validate the event. The Request Resource needs to be this
    // resource - for now
    // if
    // (!defn.getUsages().get(0).getRequestResources().get(0).equals(root.getName()))
    // throw new
    // Exception("Event "+defn.getCode()+" has a mismatched request resource - should be "+root.getName()+", is "+defn.getUsages().get(0).getRequestResources().get(0));
    if (definitions.getEvents().containsKey(defn.getCode())) {
      EventDefn master = definitions.getEvents().get(defn.getCode());
      master.getUsages().add(defn.getUsages().get(0));
      if (!defn.getDefinition().startsWith("(see"))
        master.setDefinition(defn.getDefinition());
    } else
      definitions.getEvents().put(defn.getCode(), defn);
  }

  public boolean checkFile(String purpose, String dir, String file, List<String> errors, String category)
      throws IOException
      {
    CSFile f = new CSFile(dir+file);
    if (!f.exists()) {
      errors.add("Unable to find "+purpose+" file "+file+" in "+dir);
      return false;
    } else  {
      long d = f.lastModified();
      if (!file.endsWith(".sheet.txt") && (!dates.containsKey(category) || d > dates.get(category)))
        dates.put(category, d);
      return true;
    }
      }
  private Map<String, Long> dates;
  private ValueSetGenerator vsGen;

  public void checkConditions(List<String> errors, Map<String, Long> dates) throws Exception {
    Utilities.checkFolder(srcDir, errors);
    Utilities.checkFolder(termDir, errors);
    Utilities.checkFolder(imgDir, errors);
    this.dates = dates;
    checkFile("required", termDir, "bindings.xml", errors, "all");
    checkFile("required", dtDir, "primitives.xml", errors, "all");

    for (String n : ini.getPropertyNames("types"))
      if (ini.getStringProperty("types", n).equals("")) {
        TypeRef t = new TypeParser().parse(n, false, null, context, true).get(0);
        checkFile("type definition", dtDir, t.getName().toLowerCase() + ".xml", errors, "all");
      }
    for (String n : ini.getPropertyNames("structures"))
      checkFile("structure definition", dtDir, n.toLowerCase() + ".xml",errors,"all");

    String[] shared = ini.getPropertyNames("shared");

    if(shared != null)
      for (String n : shared )
        checkFile("shared structure definition", dtDir, n.toLowerCase() + ".xml",errors,"all");

    for (String n : ini.getPropertyNames("infrastructure"))
      checkFile("infrastructure definition", dtDir, n.toLowerCase() + ".xml",	errors,"all");

    for (String n : ini.getPropertyNames("resources")) {
      if (!new File(srcDir + n).exists())
        errors.add("unable to find folder for resource "+n);
      else {
        checkFile("spreadsheet definition", srcDir + n+ File.separatorChar, n + "-spreadsheet.xml", errors, n);
        checkFile("example xml", srcDir + n + File.separatorChar,	n + "-example.xml", errors, n);
        // now iterate all the files in the directory checking data

        for (String fn : new File(srcDir + n + File.separatorChar).list())
          checkFile("source", srcDir + n + File.separatorChar, fn, errors, n);
      }
    }
    for (String n : ini.getPropertyNames("special-resources")) {
      if (new CSFile(srcDir + n + File.separatorChar + n+ "-spreadsheet.xml").exists())
        checkFile("definition", srcDir + n+ File.separatorChar, n + "-spreadsheet.xml", errors, n);
      else
        checkFile("definition", srcDir + n+ File.separatorChar, n + "-def.xml", errors, n);
      // now iterate all the files in the directory checking data
      for (String fn : new File(srcDir + n + File.separatorChar).list())
        checkFile("source", srcDir + n + File.separatorChar, fn, errors, n);
    }
  }


  public OIDRegistry getRegistry() {
    return registry;
  }




  public IniFile getIni() {
    return ini;
  }
  
  
}
