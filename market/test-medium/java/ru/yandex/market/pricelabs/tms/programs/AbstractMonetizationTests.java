package ru.yandex.market.pricelabs.tms.programs;

import java.io.File;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.model.program.AdvProgramActivationRequest;
import ru.yandex.market.pricelabs.model.program.AdvProgramActivationRequestStatus;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.processing.ExecutorSources;
import ru.yandex.market.pricelabs.tms.processing.YtScenarioExecutor;

public class AbstractMonetizationTests extends AbstractTmsSpringConfiguration {

    @Autowired
    protected ExecutorSources executors;
    protected YtScenarioExecutor<AdvProgramActivationRequest> advProgramActivationRequestYtScenarioExecutor;

    @BeforeEach
    private void superBeforeEach() {
        advProgramActivationRequestYtScenarioExecutor = executors.activationRequestExecutor();

        advProgramActivationRequestYtScenarioExecutor.clearTargetTable();
    }

    protected AdvProgramActivationRequest getRequest(AdvProgramActivationRequest request) {
        return getRequest(request.getPartner_id());
    }

    protected AdvProgramActivationRequest getRequest(int partnerId) {
        List<AdvProgramActivationRequest> activationRequests =
                advProgramActivationRequestYtScenarioExecutor.selectTargetRows();
        return activationRequests
                .stream()
                .filter(p -> p.getPartner_id() == partnerId).findFirst()
                .orElseThrow();
    }


    protected AdvProgramActivationRequest newActivationRequest(int shopId) {
        return newActivationRequest(shopId, false, AdvProgramActivationRequestStatus.NEW);
    }

    protected AdvProgramActivationRequest newActivationRequest(int shopId, boolean forReset,
                                                               AdvProgramActivationRequestStatus status) {
        AdvProgramActivationRequest r = new AdvProgramActivationRequest();
        r.setProgram_type("NEWBIE");
        r.setPartner_id(shopId);
        r.setProgram_id(shopId);
        r.setStatus(status);
        r.setUpdated_at(forReset ? Instant.now().minus(10, ChronoUnit.DAYS) : Instant.now());
        r.setSent(false);
        return r;
    }

    protected List<AdvProgramActivationRequest> getActivationRequests() {
        return List.of(
                newActivationRequest(100),
                newActivationRequest(200, true, AdvProgramActivationRequestStatus.NEW),
                newActivationRequest(300, true, AdvProgramActivationRequestStatus.NEW),
                newActivationRequest(400),
                newActivationRequest(500)
        );
    }

    protected String prettify(String json) throws Exception {
        json = json.trim();
        return json.startsWith("{") ? new JSONObject(json).toString() : new JSONArray(json).toString();
    }

    @SuppressWarnings("SameParameterValue")
    protected String loadJson(@Nonnull String path) throws Exception {
        URL file = AbstractMonetizationTests.class.getResource(path + ".json");
        return prettify(
                FileUtils.readFileToString(
                        new File(
                                Objects.requireNonNull(file).toURI()
                        )
                )
        );
    }
}
