package com.example.scheduling;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AppointmentControllerTest {

    private static final String START = "2030-01-01T09:00:00Z";
    private static final String END = "2030-01-01T10:00:00Z";
    private static final String ADJACENT_END = "2030-01-01T11:00:00Z";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void mapsBookingConflictAdjacencyCancellationAndRebooking() throws Exception {
        String providerId = "provider-" + UUID.randomUUID();
        MvcResult created = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(providerId, START, END)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.appointment.state").value("ACTIVE"))
                .andReturn();
        String appointmentId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("appointment").path("id").asText();

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(providerId, "2030-01-01T09:30:00Z", ADJACENT_END)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"));

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(providerId, END, ADJACENT_END)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/appointments/{appointmentId}", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(providerId, START, END)))
                .andExpect(status().isCreated());
    }

    @Test
    void mapsInvalidInputMissingAppointmentAndRepeatedCancellation() throws Exception {
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(" ", START, END)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_INPUT"));

        mockMvc.perform(delete("/api/appointments/{appointmentId}", "does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));

        String providerId = "provider-" + UUID.randomUUID();
        MvcResult created = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(providerId, START, END)))
                .andExpect(status().isCreated())
                .andReturn();
        String appointmentId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("appointment").path("id").asText();

        mockMvc.perform(delete("/api/appointments/{appointmentId}", appointmentId))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/appointments/{appointmentId}", appointmentId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("ALREADY_CANCELLED"));
    }

    private String bookingRequest(String providerId, String startUtc, String endUtc) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "providerId", providerId,
                "startUtc", startUtc,
                "endUtc", endUtc));
    }
}