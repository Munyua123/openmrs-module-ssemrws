package org.openmrs.module.ssemrws.web.constants;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.Patient;
import org.openmrs.module.ssemrws.web.controller.SSEMRWebServicesController;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GenerateTxCurrSummaryResponse {
	
	private final PaginateTxCurrAndTxNewPages paginateTxCurrAndTxNewPages;
	
	public GenerateTxCurrSummaryResponse(PaginateTxCurrAndTxNewPages paginateTxCurrAndTxNewPages) {
		this.paginateTxCurrAndTxNewPages = paginateTxCurrAndTxNewPages;
	}
	
	public Object generateActiveClientsSummaryResponse(ArrayList<GetTxNew.PatientEnrollmentData> patientDataList, int page,
	        int size, String totalKey, int totalCount, Date startDate, Date endDate,
	        SSEMRWebServicesController.filterCategory filterCategory,
	        Function<List<Date>, Map<String, Map<String, Integer>>> summaryGenerator) {
		
		// Step 1: Calculate the summary based on the filtered patient list using
		// enrollment dates
		List<Date> enrollmentDates = patientDataList.stream().map(GetTxNew.PatientEnrollmentData::getEnrollmentDate)
		        .filter(Objects::nonNull).collect(Collectors.toList());
		
		Map<String, Map<String, Integer>> summary = summaryGenerator.apply(enrollmentDates);
		
		// Step 2: Extract the patient list for pagination
		List<Patient> patientList = patientDataList.stream().map(GetTxNew.PatientEnrollmentData::getPatient)
		        .collect(Collectors.toList());
		
		// Step 3: Paginate the patient list
		Object paginatedResponse = paginateTxCurrAndTxNewPages.fetchAndPaginatePatientsForNewlyEnrolledPatients(patientList,
		    page, size, totalKey, totalCount, startDate, endDate, filterCategory);
		
		// Convert to ObjectNode if it's a String (which could be a JSON string)
		ObjectNode responseObj;
		if (paginatedResponse instanceof String) {
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				responseObj = (ObjectNode) objectMapper.readTree((String) paginatedResponse);
			}
			catch (Exception e) {
				// Handle the case when no data is available gracefully by returning an empty
				// response
				responseObj = JsonNodeFactory.instance.objectNode();
			}
		} else if (paginatedResponse instanceof ObjectNode) {
			responseObj = (ObjectNode) paginatedResponse;
		} else {
			// Default empty response to avoid serialization issues
			responseObj = JsonNodeFactory.instance.objectNode();
		}
		
		// Step 4: Add the summary to the paginated response if available
		if (summary != null && !summary.isEmpty()) {
			ObjectNode groupingObj = JsonNodeFactory.instance.objectNode();
			ObjectNode groupYear = JsonNodeFactory.instance.objectNode();
			
			// Populate the summary into the response
			summary.get("groupYear").forEach(groupYear::put);
			
			groupingObj.put("groupYear", groupYear);
			responseObj.put("summary", groupingObj);
		}
		
		responseObj.put(totalKey, totalCount);
		
		return responseObj.toString();
	}
}
