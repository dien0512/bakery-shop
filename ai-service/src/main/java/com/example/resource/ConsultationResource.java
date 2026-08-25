package com.example.resource;

import com.example.dto.ConsultationRequest;
import com.example.dto.ConsultationResponse;
import com.example.dto.QuestionnaireResponse;
import com.example.service.ConsultationService;
import com.example.service.QuestionnaireService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/ai/consultations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "AI Bakery Concierge")
@SecurityRequirement(name = "bearerAuth")
@RolesAllowed({"USER", "ADMIN"})
public class ConsultationResource {
    @Inject QuestionnaireService questionnaireService;
    @Inject ConsultationService consultationService;

    @GET
    @Path("/questions")
    @Operation(summary = "Get the guided AI consultation questionnaire")
    public QuestionnaireResponse getQuestions() {
        return questionnaireService.getQuestionnaire();
    }

    @POST
    @Operation(summary = "Normalize answers and recommend purchasable bakery combos")
    public ConsultationResponse consult(@Valid ConsultationRequest request) {
        return consultationService.consult(request);
    }
}
