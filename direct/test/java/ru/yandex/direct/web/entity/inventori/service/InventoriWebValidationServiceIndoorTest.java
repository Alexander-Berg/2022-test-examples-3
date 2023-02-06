package ru.yandex.direct.web.entity.inventori.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.entity.inventori.model.PageBlockWeb;
import ru.yandex.direct.web.core.entity.inventori.model.ReachIndoorRequest;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriWebValidationService;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.creativeNotFound;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.notEmptyCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@DirectWebTest
@RunWith(SpringRunner.class)
public class InventoriWebValidationServiceIndoorTest {

    @Autowired
    protected Steps steps;

    protected ClientInfo clientInfo;
    protected CreativeInfo creativeInfo;
    protected ClientId clientId;
    protected int shard;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        creativeInfo = steps.creativeSteps()
                .addDefaultCpmIndoorVideoCreative(clientInfo, steps.creativeSteps().getNextCreativeId());
    }

    @Autowired
    private InventoriWebValidationService inventoriValidationService;

    @Test
    public void validate_requestNull() {
        ValidationResult<ReachIndoorRequest, Defect> vr = inventoriValidationService.validate((ReachIndoorRequest) null, clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(), notNull()))));
    }

    @Test
    public void validate_creativeNull() {
        ReachIndoorRequest request = validRequest()
                .withVideoCreativeIds(singletonList(null));
        ValidationResult<ReachIndoorRequest, Defect> vr = inventoriValidationService.validate(request, clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("video_creative_ids"), index(0)), notNull()))));
    }

    @Test
    public void validate_creativeWrongType() {
        Long creativeId = steps.creativeSteps().addDefaultCanvasCreative(clientInfo).getCreativeId();

        ReachIndoorRequest request = validRequest()
                .withVideoCreativeIds(singletonList(creativeId));
        ValidationResult<ReachIndoorRequest, Defect> vr = inventoriValidationService.validate(request, clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("video_creative_ids"), index(0)), creativeNotFound()))));
    }

    @Test
    public void validate_creativeWrongClient() {
        ClientInfo anotherClient = steps.clientSteps().createDefaultClient();
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmIndoorVideoCreative(anotherClient, creativeId);

        ReachIndoorRequest request = validRequest()
                .withVideoCreativeIds(singletonList(creativeId));
        ValidationResult<ReachIndoorRequest, Defect> vr = inventoriValidationService.validate(request, clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("video_creative_ids"), index(0)), creativeNotFound()))));
    }

    @Test
    public void validate_creativeUnexisting() {
        Long creativeId = Long.MAX_VALUE;

        ReachIndoorRequest request = validRequest()
                .withVideoCreativeIds(singletonList(creativeId));
        ValidationResult<ReachIndoorRequest, Defect> vr = inventoriValidationService.validate(request, clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("video_creative_ids"), index(0)), creativeNotFound()))));
    }

    @Test
    public void validate_pageBlockNull() {
        ReachIndoorRequest request = validRequest()
                .withPageBlocks(singletonList(null));
        ValidationResult<ReachIndoorRequest, Defect> vr = inventoriValidationService.validate(request, clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("page_blocks"), index(0)), notNull()))));
    }

    @Test
    public void validate_pageBlockNullPage() {
        ReachIndoorRequest request = validRequest()
                .withPageBlocks(singletonList(new PageBlockWeb(null, singletonList(2L))));
        ValidationResult<ReachIndoorRequest, Defect> vr = inventoriValidationService.validate(request, clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("page_blocks"), index(0), field("page_id")), notNull()))));
    }

    @Test
    public void validate_pageBlockNullBlocks() {
        ReachIndoorRequest request = validRequest()
                .withPageBlocks(singletonList(new PageBlockWeb(1L, null)));
        ValidationResult<ReachIndoorRequest, Defect> vr = inventoriValidationService.validate(request, clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("page_blocks"), index(0), field("block_ids")), notNull()))));
    }

    @Test
    public void validate_pageBlockEmptyBlocks() {
        ReachIndoorRequest request = validRequest()
                .withPageBlocks(singletonList(new PageBlockWeb(1L, emptyList())));
        ValidationResult<ReachIndoorRequest, Defect> vr = inventoriValidationService.validate(request, clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("page_blocks"), index(0), field("block_ids")), notEmptyCollection()))));
    }

    @Test
    public void validate_pageBlockNullBlock() {
        ReachIndoorRequest request = validRequest()
                .withPageBlocks(singletonList(new PageBlockWeb(1L, singletonList(null))));
        ValidationResult<ReachIndoorRequest, Defect> vr = inventoriValidationService.validate(request, clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("page_blocks"), index(0), field("block_ids"), index(0)), notNull()))));
    }

    private ReachIndoorRequest validRequest() {
        return new ReachIndoorRequest();
    }
}
