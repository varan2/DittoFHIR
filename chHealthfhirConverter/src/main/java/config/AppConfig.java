package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.dstu3.context.SimpleWorkerContext;
import org.hl7.fhir.dstu3.formats.JsonParser;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.ConceptMap;
import org.hl7.fhir.dstu3.model.NamingSystem;
import org.hl7.fhir.dstu3.model.OperationDefinition;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.StructureMap;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.utils.StructureMapUtilities;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

	@Value("${org.hl7.fhir.json.definitions}")
	private String definitionsPath;
	
	@Value("${structure.definitions}")
	private String structureDefinitionsPath;
	
	@Value("${mappings}")
	private String mappingPath;

	private Map<String, StructureMap> maps = new HashMap<String, StructureMap>();

	@Bean
	public SimpleWorkerContext getWorkerContext() throws FileNotFoundException, IOException, FHIRException {
		SimpleWorkerContext ctx = SimpleWorkerContext.fromPack(definitionsPath);

		// Load all costum structure definitions
		for (String f : new File(Utilities.path(structureDefinitionsPath)).list()) {
			try {
				Resource r = new JsonParser()
						.parse(new FileInputStream(Utilities.path(structureDefinitionsPath, f)));
				
				String url = "";
				
				if (r instanceof StructureDefinition)
					url = ((StructureDefinition) r).getUrl();
				    else if (r instanceof ValueSet)
				    	url = ((ValueSet) r).getUrl();
				    else if (r instanceof CodeSystem)
				    	url = ((CodeSystem) r).getUrl();
				    else if (r instanceof OperationDefinition)
				    	url = ((OperationDefinition) r).getUrl();
				    else if (r instanceof ConceptMap)
				    	url = ((ConceptMap) r).getUrl();
				    else if (r instanceof StructureMap)
				    	url = ((StructureMap) r).getUrl();
				    else if (r instanceof NamingSystem)
				    	url = ""; // NamingSystem has no url field.
				
				ctx.seeResource(url, r);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ctx;
	}

	@Bean
	public StructureMapUtilities getStructureMapUtilities(SimpleWorkerContext simpleWorkerContext) throws IOException, FHIRException {
		StructureMapUtilities structureMapUtilities = new StructureMapUtilities(simpleWorkerContext, maps, null);
		readMaps(structureMapUtilities);
		return structureMapUtilities;
	}


	 private void readMaps(StructureMapUtilities structureMapUtilites) throws IOException, FHIRException {
 			for (String f : new File(Utilities.path(mappingPath)).list()) {
					StructureMap map = structureMapUtilites
							.parse(TextFile.fileToString(Utilities.path(mappingPath, f)));
					maps.put(map.getUrl(), map);
			}
	}
}
