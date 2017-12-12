package controller;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.hl7.fhir.dstu3.context.SimpleWorkerContext;
import org.hl7.fhir.dstu3.elementmodel.Element;
import org.hl7.fhir.dstu3.elementmodel.Manager;
import org.hl7.fhir.dstu3.elementmodel.Manager.FhirFormat;
import org.hl7.fhir.dstu3.formats.IParser.OutputStyle;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.utils.StructureMapUtilities;
import org.hl7.fhir.exceptions.DefinitionException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.FHIRFormatError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConverterController {

	@Autowired
	SimpleWorkerContext ctx;

	@Autowired
	StructureMapUtilities structureMapUtilities;

	@PostMapping("/eMediplan")
	public void emediplanToFhir(HttpServletRequest request, HttpServletResponse response)
			throws FHIRFormatError, DefinitionException, IOException, FHIRException {
		// TODO get request body and translate it to FHIR and return the result
		InputStream inputStream = request.getInputStream();
		String message = IOUtils.toString(inputStream);
		message = message.replaceFirst("\\{", "{ \"resourceType\": \"eMediplanMedication\",");

		System.out.println(message);
		Element source = Manager.parse(ctx, IOUtils.toInputStream(message), FhirFormat.JSON);

		for (Element s : source.getChildren()) {
			System.out.println(s);
		}

		System.out.println(source.toString());
		System.out.println(source.getChildValue("Auth"));

		Element target = Manager.build(ctx,
				ctx.fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/CarePlan"));

		structureMapUtilities.transform(null, source,
				structureMapUtilities.getLibrary().get("http://hl7.org/fhir/StructureMap/eMediplanToFhir"), target);

		Manager.compose(ctx, target, response.getOutputStream(), FhirFormat.JSON, OutputStyle.PRETTY, null);
	}
	
	@PostMapping("/PatientId")
	public void patientId(HttpServletRequest request, HttpServletResponse response)
			throws FHIRFormatError, DefinitionException, IOException, FHIRException {
		// TODO get request body and translate it to FHIR and return the result
		InputStream inputStream = request.getInputStream();
		String message = IOUtils.toString(inputStream);
		message = message.replaceFirst("\\{", "{ \"resourceType\": \"eMediplanPatientId\",");

		Element source = Manager.parse(ctx, IOUtils.toInputStream(message), FhirFormat.JSON);

		Element target = Manager.build(ctx,
				ctx.fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/Identifier"));

		structureMapUtilities.transform(null, source,
				structureMapUtilities.getLibrary().get("http://hl7.org/fhir/StructureMap/eMediplanPatientIdToIdentifier"), target);

		Manager.compose(ctx, target, response.getOutputStream(), FhirFormat.JSON, OutputStyle.PRETTY, null);
	}
	
	@PostMapping("/Measurement")
	public void measurement(HttpServletRequest request, HttpServletResponse response)
			throws FHIRFormatError, DefinitionException, IOException, FHIRException {
		// TODO get request body and translate it to FHIR and return the result
		InputStream inputStream = request.getInputStream();
		String message = IOUtils.toString(inputStream);
		message = message.replaceFirst("\\{", "{ \"resourceType\": \"eMediplanMeasurement\",");

		Element source = Manager.parse(ctx, IOUtils.toInputStream(message), FhirFormat.JSON);

		Element target = Manager.build(ctx ,
				ctx.fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/Observation"));

		structureMapUtilities.transform(null, source,
				structureMapUtilities.getLibrary().get("http://hl7.org/fhir/StructureMap/eMediplanMeasurementToObservation"), target);

		Manager.compose(ctx, target, response.getOutputStream(), FhirFormat.JSON, OutputStyle.PRETTY, null);
	}
	
	@PostMapping("/Patient")
	public void patient(HttpServletRequest request, HttpServletResponse response)
			throws FHIRFormatError, DefinitionException, IOException, FHIRException {
		// TODO get request body and translate it to FHIR and return the result
		InputStream inputStream = request.getInputStream();
		String message = IOUtils.toString(inputStream);
		message = message.replaceFirst("\\{", "{ \"resourceType\": \"eMediplanPatient\",");

		Element source = Manager.parse(ctx, IOUtils.toInputStream(message), FhirFormat.JSON);

		Element target = Manager.build(ctx ,
				ctx.fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/Patient"));

		structureMapUtilities.transform(null, source,
				structureMapUtilities.getLibrary().get("http://hl7.org/fhir/StructureMap/eMediplanPatientToFhir"), target);

		Manager.compose(ctx, target, response.getOutputStream(), FhirFormat.JSON, OutputStyle.PRETTY, null);
	}
	
	@PostMapping("/Medicament")
	public void medicament(HttpServletRequest request, HttpServletResponse response)
			throws FHIRFormatError, DefinitionException, IOException, FHIRException {
		// TODO get request body and translate it to FHIR and return the result
		InputStream inputStream = request.getInputStream();
		String message = IOUtils.toString(inputStream);
		message = message.replaceFirst("\\{", "{ \"resourceType\": \"eMediplanMedicament\",");

		Element source = Manager.parse(ctx, IOUtils.toInputStream(message), FhirFormat.JSON);

		Element target = Manager.build(ctx ,
				ctx.fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/MedicationRequest"));

		structureMapUtilities.transform(null, source,
				structureMapUtilities.getLibrary().get("http://hl7.org/fhir/StructureMap/eMediplanMedicamentToFhirMedicationRequest"), target);

		Manager.compose(ctx, target, response.getOutputStream(), FhirFormat.JSON, OutputStyle.PRETTY, null);
	}
	
	@PostMapping("/RiskCategory")
	public void riskCategory(HttpServletRequest request, HttpServletResponse response)
			throws FHIRFormatError, DefinitionException, IOException, FHIRException {
		// TODO get request body and translate it to FHIR and return the result
		InputStream inputStream = request.getInputStream();
		String message = IOUtils.toString(inputStream);
		message = message.replaceFirst("\\{", "{ \"resourceType\": \"eMediplanRiskCategory\",");

		Element source = Manager.parse(ctx, IOUtils.toInputStream(message), FhirFormat.JSON);

		Element target = Manager.build(ctx ,
				ctx.fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/AllergyIntolerance"));

		structureMapUtilities.transform(null, source,
				structureMapUtilities.getLibrary().get("http://hl7.org/fhir/StructureMap/eMediplanRiskCategoryToAllergyIntolerance"), target);

		Manager.compose(ctx, target, response.getOutputStream(), FhirFormat.JSON, OutputStyle.PRETTY, null);
	}
	
	@PostMapping("/MedicalData")
	public void medicalData(HttpServletRequest request, HttpServletResponse response)
			throws FHIRFormatError, DefinitionException, IOException, FHIRException {
		// TODO get request body and translate it to FHIR and return the result
		InputStream inputStream = request.getInputStream();
		String message = IOUtils.toString(inputStream);
		message = message.replaceFirst("\\{", "{ \"resourceType\": \"eMediplanMedicalData\",");

		Element source = Manager.parse(ctx, IOUtils.toInputStream(message), FhirFormat.JSON);

		Element target = Manager.build(ctx ,
				ctx.fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/Observation"));

		structureMapUtilities.transform(null, source,
				structureMapUtilities.getLibrary().get("http://hl7.org/fhir/StructureMap/eMediplanMedicalDataToFhir"), target);

		Manager.compose(ctx, target, response.getOutputStream(), FhirFormat.JSON, OutputStyle.PRETTY, null);
	}

	public void setCtx(SimpleWorkerContext ctx) {
		this.ctx = ctx;
	}

	public void setStructureMapUtilities(StructureMapUtilities structureMapUtilities) {
		this.structureMapUtilities = structureMapUtilities;
	}

}
