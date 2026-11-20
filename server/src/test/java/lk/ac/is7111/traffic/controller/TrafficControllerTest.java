package lk.ac.is7111.traffic.controller;

import lk.ac.is7111.traffic.service.TrafficService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TrafficControllerTest {
    @Autowired MockMvc mvc;
    @Autowired TrafficService service;
    @BeforeEach void reset() { service.reset(); }

    @Test void initialStatusContract() throws Exception {
        mvc.perform(get("/api/traffic/status")).andExpect(status().isOk())
            .andExpect(jsonPath("$.currentLight").value("RED"))
            .andExpect(jsonPath("$.mode").value("STOPPED"))
            .andExpect(jsonPath("$.remainingSeconds").value(0))
            .andExpect(jsonPath("$.running").value(false))
            .andExpect(jsonPath("$.emergency").value(false))
            .andExpect(jsonPath("$.message").isString());
    }
    @Test void automaticEndpointsReturnState() throws Exception {
        mvc.perform(post("/api/traffic/start")).andExpect(status().isOk())
            .andExpect(jsonPath("$.currentLight").value("RED"))
            .andExpect(jsonPath("$.remainingSeconds").value(10));
        mvc.perform(post("/api/traffic/pause")).andExpect(status().isOk())
            .andExpect(jsonPath("$.mode").value("PAUSED"));
        mvc.perform(post("/api/traffic/resume")).andExpect(status().isOk())
            .andExpect(jsonPath("$.running").value(true));
        mvc.perform(post("/api/traffic/stop")).andExpect(status().isOk())
            .andExpect(jsonPath("$.mode").value("STOPPED"))
            .andExpect(jsonPath("$.remainingSeconds").value(0));
    }
    @Test void invalidResumeReturns409AndUnchangedState() throws Exception {
        mvc.perform(post("/api/traffic/resume")).andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").isString())
            .andExpect(jsonPath("$.state.mode").value("STOPPED"));
        mvc.perform(get("/api/traffic/status")).andExpect(jsonPath("$.remainingSeconds").value(0));
    }
    @Test void invalidLightReturns400AndPreservesManualSelection() throws Exception {
        mvc.perform(post("/api/traffic/manual/GREEN")).andExpect(status().isOk())
            .andExpect(jsonPath("$.mode").value("MANUAL"));
        mvc.perform(post("/api/traffic/manual/BLUE")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.state.currentLight").value("GREEN"));
    }
    @Test void emergencyCannotBeBypassedThroughRest() throws Exception {
        mvc.perform(post("/api/traffic/emergency")).andExpect(status().isOk())
            .andExpect(jsonPath("$.emergency").value(true));
        mvc.perform(post("/api/traffic/start")).andExpect(status().isConflict());
        mvc.perform(post("/api/traffic/manual/GREEN")).andExpect(status().isConflict());
        mvc.perform(post("/api/traffic/stop")).andExpect(status().isOk())
            .andExpect(jsonPath("$.mode").value("STOPPED"))
            .andExpect(jsonPath("$.emergency").value(true));
        mvc.perform(post("/api/traffic/start")).andExpect(status().isConflict());
        mvc.perform(post("/api/traffic/reset")).andExpect(status().isOk())
            .andExpect(jsonPath("$.emergency").value(false));
        mvc.perform(post("/api/traffic/start")).andExpect(status().isOk());
    }
    @Test void manualWhileRunningReturns409() throws Exception {
        mvc.perform(post("/api/traffic/start")).andExpect(status().isOk());
        mvc.perform(post("/api/traffic/manual/RED")).andExpect(status().isConflict());
    }
    @Test void unsupportedMethodAndRouteRejected() throws Exception {
        mvc.perform(get("/api/traffic/start")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/traffic/unknown")).andExpect(status().isNotFound());
    }
}
