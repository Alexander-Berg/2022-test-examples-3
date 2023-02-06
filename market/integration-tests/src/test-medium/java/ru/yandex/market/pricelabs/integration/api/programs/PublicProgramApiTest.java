package ru.yandex.market.pricelabs.integration.api.programs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.pricelabs.api.api.PublicProgramApi;
import ru.yandex.market.pricelabs.generated.server.pub.model.OkResponse;
import ru.yandex.market.pricelabs.generated.server.pub.model.ProgramActivationRequest;
import ru.yandex.market.pricelabs.integration.api.AbstractApiTests;
import ru.yandex.market.pricelabs.model.program.AdvProgramActivationRequest;
import ru.yandex.market.pricelabs.model.program.AdvProgramActivationRequestStatus;
import ru.yandex.market.pricelabs.model.program.MonetizationServiceImpl;
import ru.yandex.market.pricelabs.processing.CoreTables;
import ru.yandex.market.pricelabs.tms.processing.YtScenarioExecutor;
import ru.yandex.market.pricelabs.yt.YtConfiguration;

/**
 * Date: 13.07.2022
 * Project: arcadia-market_pricelabs
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
public class PublicProgramApiTest extends AbstractApiTests {

    @Autowired
    private PublicProgramApi publicProgramApi;
    @Autowired
    private MonetizationServiceImpl monetizationService;
    @Autowired
    private YtConfiguration configuration;
    @Autowired
    private CoreTables core;

    @Test
    void programsActivationPost_list_success() {
        YtScenarioExecutor<AdvProgramActivationRequest> executor =
                YtScenarioExecutor.from(
                        configuration.getProcessorCfg(
                                core.getAdvProgramActivationRequest()
                        )
                );
        executor.clearTargetTable();

        ResponseEntity<OkResponse> okResponseResponseEntity = publicProgramApi.programsActivationPost(
                List.of(
                        createRequest(41L, 431, "SORRY"),
                        createRequest(45L, 431, "NEWBIE"),
                        createRequest(46L, 652, "MANUAL"),
                        createRequest(49L, 431, "AUTO")
                )
        );
        Assertions.assertThat(okResponseResponseEntity.getStatusCode().is2xxSuccessful())
                .isTrue();
        Assertions.assertThat(okResponseResponseEntity.getBody())
                .isNotNull()
                .extracting(OkResponse::getStatus)
                .isEqualTo("OK");
        monetizationService.onProgramRequestReset();

        List<AdvProgramActivationRequest> actualRows = new ArrayList<>();

        for (AdvProgramActivationRequest request : executor.selectTargetRows()) {
            request.setUpdated_at(null);
            actualRows.add(request);
        }

        Assertions.assertThat(actualRows)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        createResult(41L, 431, "SORRY"),
                        createResult(45L, 431, "NEWBIE"),
                        createResult(46L, 652, "MANUAL"),
                        createResult(49L, 431, "AUTO")

                );
    }

    @Nonnull
    private ProgramActivationRequest createRequest(long programId, int shopId, String programType) {
        ProgramActivationRequest request = new ProgramActivationRequest();

        request.setProgramId(programId);
        request.setProgramType(programType);
        request.setShopId(shopId);

        return request;
    }

    @Nonnull
    private AdvProgramActivationRequest createResult(long programId, int shopId, String programType) {
        AdvProgramActivationRequest request = new AdvProgramActivationRequest();

        request.setPartner_id(shopId);
        request.setProgram_type(programType.toUpperCase(Locale.ROOT));
        request.setProgram_id(programId);
        request.setStatus(AdvProgramActivationRequestStatus.NEW);
        request.setUpdated_at(null);
        request.setSent(false);

        return request;
    }
}
