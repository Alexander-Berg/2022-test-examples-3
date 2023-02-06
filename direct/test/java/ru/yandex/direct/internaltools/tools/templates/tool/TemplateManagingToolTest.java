package ru.yandex.direct.internaltools.tools.templates.tool;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import junitparams.converters.Nullable;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.internalads.model.DirectTemplate;
import ru.yandex.direct.core.entity.internalads.model.DirectTemplateResource;
import ru.yandex.direct.core.entity.internalads.model.DirectTemplateResourceOption;
import ru.yandex.direct.core.entity.internalads.model.DirectTemplateState;
import ru.yandex.direct.core.entity.internalads.model.TemplatePlace;
import ru.yandex.direct.core.entity.internalads.repository.DirectTemplatePlaceRepository;
import ru.yandex.direct.core.entity.internalads.repository.DirectTemplateRepository;
import ru.yandex.direct.core.entity.internalads.repository.DirectTemplateResourceRepository;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.templates.model.TemplateAction;
import ru.yandex.direct.internaltools.tools.templates.model.TemplateInput;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.direct.internaltools.tools.templates.tool.TemplateManagingTool.TEMPLATE_STRING;
import static ru.yandex.direct.internaltools.tools.templates.tool.TemplateUtil.MIN_NEW_TEMPLATE_ID;
import static ru.yandex.direct.internaltools.tools.templates.tool.TemplateUtil.SPECIFIC_VALUE_TO_CLEAR_PLACE_IDS;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TemplateManagingToolTest {
    private static final Long TEMPLATE_ID_OLD = RandomNumberUtils.nextPositiveLong(MIN_NEW_TEMPLATE_ID);
    private static final String FORMAT_NAME_OLD = "old template";
    private static final String FORMAT_NAME_NEW = " new template  ";
    private static final String FORMAT_NAME_NEW_TRIMMED = "new template";
    private static final DirectTemplateState STATE_OLD = DirectTemplateState.DEFAULT;
    private static final DirectTemplateState STATE_NEW = DirectTemplateState.TRANSITIONAL;
    private static final DirectTemplate DIRECT_TEMPLATE_MIGRATED = new DirectTemplate()
            .withDirectTemplateId(TEMPLATE_ID_OLD)
            .withFormatName(FORMAT_NAME_OLD)
            .withState(STATE_OLD);

    // Номера плейсов захардкожены в PlaceRepositoryMockUtils
    private static final Long PLACE_ID1 = 2L;
    private static final Long PLACE_ID2 = 3L;
    private static final TemplatePlace TEMPLATE_PLACE_MIGRATED1 = new TemplatePlace()
            .withPlaceId(PLACE_ID1)
            .withTemplateId(TEMPLATE_ID_OLD);
    private static final TemplatePlace TEMPLATE_PLACE_MIGRATED2 = new TemplatePlace()
            .withPlaceId(PLACE_ID2)
            .withTemplateId(TEMPLATE_ID_OLD);

    // Номера ресурсов захардкожены в TemplateResourceRepositoryMockUtils
    private static final Long UNIFIED_RESOURCE_NO1 = 7L;
    private static final Long UNIFIED_RESOURCE_NO2 = 17L;
    private static final Long UNIFIED_RESOURCE_NO3 = 67L;
    private static final Long UNIFIED_RESOURCE_NO4 = 73L;

    private static final Long TEMPLATE_RESOURCE_ID_OLD1 = 3876L;
    private static final Long TEMPLATE_RESOURCE_ID_OLD2 = 1442L;
    private static final Long RESOURCE_NO1 = 20L;
    private static final Long RESOURCE_NO2 = 55L;
    private static final Long UNIFIED_TEMPLATE_RESOURCE_ID1 = 5970L; // Взято у resourceNo=7
    private static final Long UNIFIED_TEMPLATE_RESOURCE_ID2 = 5980L; // Взято у resourceNo=17
    private static final Long UNIFIED_TEMPLATE_RESOURCE_ID3 = 6030L; // Взято у resourceNo=67
    private static final Long UNIFIED_TEMPLATE_RESOURCE_ID4 = 6036L; // Взято у resourceNo=73
    private static final Set<DirectTemplateResourceOption> EMPTY_OPTIONS = Set.of();
    private static final Set<DirectTemplateResourceOption> REQUIRED_OPTIONS =
            Set.of(DirectTemplateResourceOption.REQUIRED);
    private static final Set<DirectTemplateResourceOption> OPTIONS2_WITHOUT_REQUIRED =
            Set.of(DirectTemplateResourceOption.BANANA_IMAGE);
    private static final Set<DirectTemplateResourceOption> OPTIONS2_WITH_REQUIRED =
            Set.of(DirectTemplateResourceOption.BANANA_IMAGE, DirectTemplateResourceOption.REQUIRED);
    private static final Set<DirectTemplateResourceOption> OPTIONS3_WITHOUT_REQUIRED =
            Set.of(DirectTemplateResourceOption.BANANA_URL);
    private static final Set<DirectTemplateResourceOption> OPTIONS3_WITH_REQUIRED =
            Set.of(DirectTemplateResourceOption.BANANA_URL, DirectTemplateResourceOption.REQUIRED);

    private static final DirectTemplateResource TEMPLATE_RESOURCE_MIGRATED1 = new DirectTemplateResource()
            .withDirectTemplateResourceId(TEMPLATE_RESOURCE_ID_OLD1)
            .withDirectTemplateId(TEMPLATE_ID_OLD)
            .withResourceNo(RESOURCE_NO1)
            .withUnifiedResourceNo(UNIFIED_RESOURCE_NO1)
            .withUnifiedTemplateResourceId(UNIFIED_TEMPLATE_RESOURCE_ID1)
            .withOptions(EMPTY_OPTIONS);
    private static final DirectTemplateResource TEMPLATE_RESOURCE_MIGRATED2 = new DirectTemplateResource()
            .withDirectTemplateResourceId(TEMPLATE_RESOURCE_ID_OLD2)
            .withDirectTemplateId(TEMPLATE_ID_OLD)
            .withResourceNo(RESOURCE_NO2)
            .withUnifiedResourceNo(UNIFIED_RESOURCE_NO2)
            .withUnifiedTemplateResourceId(UNIFIED_TEMPLATE_RESOURCE_ID2)
            .withOptions(OPTIONS2_WITH_REQUIRED);

    private static final Pattern TEMPLATE_COPIED_MESSAGE_PATTERN = Pattern.compile(
            "^Template (\\d+) has been successfully copied from template (\\d+)$");
    private static final Pattern TEMPLATE_CREATED_MESSAGE_PATTERN = Pattern.compile(
            "^Template (\\d+) has been successfully created$");

    @Autowired
    private TemplateManagingTool templateManagingTool;

    @Autowired
    private DirectTemplateRepository directTemplateRepository;

    @Autowired
    DirectTemplatePlaceRepository directTemplatePlaceRepository;

    @Autowired
    DirectTemplateResourceRepository directTemplateResourceRepository;

    @Before
    public void before() {
        directTemplateRepository.delete(TEMPLATE_ID_OLD);
        directTemplateRepository.addOldTemplate(DIRECT_TEMPLATE_MIGRATED);

        directTemplatePlaceRepository.add(List.of(TEMPLATE_PLACE_MIGRATED1));
        directTemplatePlaceRepository.add(List.of(TEMPLATE_PLACE_MIGRATED2));

        var directTemplateResourceIds = mapList(
                directTemplateResourceRepository.getByTemplateIds(List.of(TEMPLATE_ID_OLD)),
                DirectTemplateResource::getDirectTemplateResourceId);
        directTemplateResourceRepository.delete(directTemplateResourceIds);
        directTemplateResourceRepository.addOrUpdate(List.of(TEMPLATE_RESOURCE_MIGRATED1, TEMPLATE_RESOURCE_MIGRATED2));
    }

    @Test
    public void processUpdate_AllFields_ChangeResourceRequirementsAndOrder() {
        var input = new TemplateInput()
                .withTemplateAction(TemplateAction.UPDATE)
                .withDirectTemplateId(TEMPLATE_ID_OLD)
                .withFormatName(FORMAT_NAME_NEW)
                .withState(STATE_NEW)
                .withPlaceIds(PLACE_ID2.toString())
                .withResources(UNIFIED_RESOURCE_NO2 + " 0\n" + UNIFIED_RESOURCE_NO1 + " 1");

        var result = templateManagingTool.process(input);
        var updatedTemplate = directTemplateRepository.get(List.of(TEMPLATE_ID_OLD)).get(TEMPLATE_ID_OLD);
        var updatedPlaces = directTemplatePlaceRepository.getByTemplateId(TEMPLATE_ID_OLD);
        var updatedResources = directTemplateResourceRepository.getByTemplateIds(List.of(TEMPLATE_ID_OLD));
        var resourcesByUnifiedResourceNo = listToMap(updatedResources, DirectTemplateResource::getUnifiedResourceNo);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getMessage()).isEqualTo(TEMPLATE_STRING + TEMPLATE_ID_OLD
                    + " has been successfully updated");
            softly.assertThat(updatedTemplate).isNotNull();
            softly.assertThat(updatedPlaces).hasSize(1);
            softly.assertThat(resourcesByUnifiedResourceNo).hasSize(2);
        });

        checkDirectTemplate(updatedTemplate, TEMPLATE_ID_OLD, FORMAT_NAME_NEW_TRIMMED, STATE_NEW);
        checkTemplatePlaces(updatedPlaces, TEMPLATE_ID_OLD, List.of(PLACE_ID2));
        checkDirectTemplateResource(resourcesByUnifiedResourceNo, UNIFIED_RESOURCE_NO1, TEMPLATE_ID_OLD, RESOURCE_NO1,
                UNIFIED_TEMPLATE_RESOURCE_ID1, REQUIRED_OPTIONS, TEMPLATE_RESOURCE_ID_OLD1);
        checkDirectTemplateResource(resourcesByUnifiedResourceNo, UNIFIED_RESOURCE_NO2, TEMPLATE_ID_OLD, RESOURCE_NO2,
                UNIFIED_TEMPLATE_RESOURCE_ID2, OPTIONS2_WITHOUT_REQUIRED, TEMPLATE_RESOURCE_ID_OLD2);
    }

    @Test
    public void processUpdate_PlaceIds_RemoveAll() {
        var input = new TemplateInput()
                .withTemplateAction(TemplateAction.UPDATE)
                .withDirectTemplateId(TEMPLATE_ID_OLD)
                .withPlaceIds(String.valueOf(SPECIFIC_VALUE_TO_CLEAR_PLACE_IDS));

        var result = templateManagingTool.process(input);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getMessage()).isEqualTo(TEMPLATE_STRING + TEMPLATE_ID_OLD
                    + " has been successfully updated");
            softly.assertThat(directTemplateRepository.get(List.of(TEMPLATE_ID_OLD))).hasSize(1);
            softly.assertThat(directTemplatePlaceRepository.getByTemplateId(TEMPLATE_ID_OLD)).hasSize(0);
            softly.assertThat(directTemplateResourceRepository.getByTemplateIds(
                    List.of(TEMPLATE_ID_OLD))).hasSize(2);
        });
    }

    @Test
    public void processUpdate_Resources_RemoveOneMigratedAndAddTwoUnified_InputOrderIsPreserved() {
        var input = new TemplateInput()
                .withTemplateAction(TemplateAction.UPDATE)
                .withDirectTemplateId(TEMPLATE_ID_OLD)
                .withResources(UNIFIED_RESOURCE_NO4 + " 1\n" + UNIFIED_RESOURCE_NO2 + " 0\n"
                        + UNIFIED_RESOURCE_NO3 + " 1");

        var result = templateManagingTool.process(input);
        var updatedResources = directTemplateResourceRepository.getByTemplateIds(List.of(TEMPLATE_ID_OLD));
        var resourcesByUnifiedResourceNo = listToMap(updatedResources, DirectTemplateResource::getUnifiedResourceNo);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getMessage()).isEqualTo(TEMPLATE_STRING + TEMPLATE_ID_OLD
                    + " has been successfully updated");
            softly.assertThat(directTemplateRepository.get(List.of(TEMPLATE_ID_OLD))).hasSize(1);
            softly.assertThat(directTemplatePlaceRepository.getByTemplateId(TEMPLATE_ID_OLD)).hasSize(2);
            softly.assertThat(resourcesByUnifiedResourceNo).hasSize(3);
        });

        checkDirectTemplateResource(resourcesByUnifiedResourceNo, UNIFIED_RESOURCE_NO2, TEMPLATE_ID_OLD, RESOURCE_NO2,
                UNIFIED_TEMPLATE_RESOURCE_ID2, OPTIONS2_WITHOUT_REQUIRED, TEMPLATE_RESOURCE_ID_OLD2);
        // номера новых ресурсов в мигрированных шаблонах считаются как сумма максимального номера ресурса,
        // существовавшего на шаблоне на начало операции, и номера создаваемого ресурса единого шаблона
        var maxResourceNo = Math.max(RESOURCE_NO2, RESOURCE_NO1);
        checkDirectTemplateResource(resourcesByUnifiedResourceNo, UNIFIED_RESOURCE_NO3, TEMPLATE_ID_OLD,
                UNIFIED_RESOURCE_NO3 + maxResourceNo, UNIFIED_TEMPLATE_RESOURCE_ID3, OPTIONS3_WITH_REQUIRED, null);
        checkDirectTemplateResource(resourcesByUnifiedResourceNo, UNIFIED_RESOURCE_NO4, TEMPLATE_ID_OLD,
                UNIFIED_RESOURCE_NO4 + maxResourceNo, UNIFIED_TEMPLATE_RESOURCE_ID4, REQUIRED_OPTIONS, null);
        // input order of new resources is preserved via template_resource_id:
        assertThat(resourcesByUnifiedResourceNo.get(UNIFIED_RESOURCE_NO4).getDirectTemplateResourceId(),
                lessThan(resourcesByUnifiedResourceNo.get(UNIFIED_RESOURCE_NO3).getDirectTemplateResourceId()));
    }

    @Test
    public void processCopy() {
        var input = new TemplateInput()
                .withTemplateAction(TemplateAction.COPY)
                .withDirectTemplateId(TEMPLATE_ID_OLD);

        var result = templateManagingTool.process(input);
        var messageMatcher = TEMPLATE_COPIED_MESSAGE_PATTERN.matcher(result.getMessage());
        if (!messageMatcher.find()) {
            throw new AssertionError("Cannot match copied template id in TemplateManagingTool message");
        }
        var copiedTemplateId = Long.valueOf(messageMatcher.group(1));
        var originalTemplateId = Long.valueOf(messageMatcher.group(2));
        assertThat(originalTemplateId, equalTo(TEMPLATE_ID_OLD));

        var dbTemplates = directTemplateRepository.get(List.of(TEMPLATE_ID_OLD, copiedTemplateId));
        var dbPlaces = directTemplatePlaceRepository.getByTemplateIds(List.of(TEMPLATE_ID_OLD, copiedTemplateId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getMessage()).isEqualTo(TEMPLATE_STRING + copiedTemplateId
                    + " has been successfully copied from template " + TEMPLATE_ID_OLD);
            softly.assertThat(dbTemplates).hasSize(2);
            softly.assertThat(dbPlaces).hasSize(2);
            softly.assertThat(directTemplateResourceRepository.getByTemplateIds(
                    List.of(TEMPLATE_ID_OLD, copiedTemplateId))).hasSize(4);
        });

        var copiedTemplate = dbTemplates.get(copiedTemplateId);
        checkDirectTemplate(copiedTemplate, copiedTemplateId, FORMAT_NAME_OLD, DirectTemplateState.UNIFIED);

        var copiedPlaces = dbPlaces.get(copiedTemplateId);
        assertThat(copiedPlaces, hasSize(2));
        checkTemplatePlaces(copiedPlaces, copiedTemplateId, List.of(PLACE_ID1, PLACE_ID2));

        var copiedResources = directTemplateResourceRepository.getByTemplateIds(List.of(copiedTemplateId));
        var resourcesByUnifiedResourceNo = listToMap(copiedResources, DirectTemplateResource::getUnifiedResourceNo);
        assertThat(resourcesByUnifiedResourceNo.keySet(), hasSize(2));
        checkDirectTemplateResource(resourcesByUnifiedResourceNo, UNIFIED_RESOURCE_NO1, copiedTemplateId, RESOURCE_NO1,
                UNIFIED_TEMPLATE_RESOURCE_ID1, EMPTY_OPTIONS, null);
        checkDirectTemplateResource(resourcesByUnifiedResourceNo, UNIFIED_RESOURCE_NO2, copiedTemplateId, RESOURCE_NO2,
                UNIFIED_TEMPLATE_RESOURCE_ID2, OPTIONS2_WITH_REQUIRED, null);
    }

    @Test
    public void processCreate() {
        var input = new TemplateInput()
                .withTemplateAction(TemplateAction.CREATE)
                .withFormatName(FORMAT_NAME_NEW)
                .withPlaceIds(PLACE_ID1.toString())
                .withResources(UNIFIED_RESOURCE_NO3 + " 0\n" + UNIFIED_RESOURCE_NO2 + " 1");

        var result = templateManagingTool.process(input);
        var messageMatcher = TEMPLATE_CREATED_MESSAGE_PATTERN.matcher(result.getMessage());
        if (!messageMatcher.find()) {
            throw new AssertionError("Cannot match created template id in TemplateManagingTool message");
        }
        var createdTemplateId = Long.valueOf(messageMatcher.group(1));

        var dbTemplates = directTemplateRepository.get(List.of(TEMPLATE_ID_OLD, createdTemplateId));
        var dbPlaces = directTemplatePlaceRepository.getByTemplateIds(List.of(TEMPLATE_ID_OLD, createdTemplateId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getMessage()).isEqualTo(TEMPLATE_STRING + createdTemplateId
                    + " has been successfully created");
            softly.assertThat(dbTemplates).hasSize(2);
            softly.assertThat(dbPlaces).hasSize(2);
            softly.assertThat(directTemplateResourceRepository.getByTemplateIds(
                    List.of(TEMPLATE_ID_OLD, createdTemplateId))).hasSize(4);
        });

        var createdTemplate = dbTemplates.get(createdTemplateId);
        checkDirectTemplate(createdTemplate, createdTemplateId, FORMAT_NAME_NEW_TRIMMED, DirectTemplateState.UNIFIED);

        var createdPlaces = dbPlaces.get(createdTemplateId);
        assertThat(createdPlaces, hasSize(1));
        checkTemplatePlaces(createdPlaces, createdTemplateId, List.of(PLACE_ID1));

        var createdResources = directTemplateResourceRepository.getByTemplateIds(List.of(createdTemplateId));
        var resourcesByUnifiedResourceNo = listToMap(createdResources, DirectTemplateResource::getUnifiedResourceNo);
        assertThat(resourcesByUnifiedResourceNo.keySet(), hasSize(2));
        checkDirectTemplateResource(resourcesByUnifiedResourceNo, UNIFIED_RESOURCE_NO2, createdTemplateId,
                UNIFIED_RESOURCE_NO2, UNIFIED_TEMPLATE_RESOURCE_ID2, OPTIONS2_WITH_REQUIRED, null);
        checkDirectTemplateResource(resourcesByUnifiedResourceNo, UNIFIED_RESOURCE_NO3, createdTemplateId,
                UNIFIED_RESOURCE_NO3, UNIFIED_TEMPLATE_RESOURCE_ID3, OPTIONS3_WITHOUT_REQUIRED, null);
        // input order of new resources is preserved via template_resource_id:
        assertThat(resourcesByUnifiedResourceNo.get(UNIFIED_RESOURCE_NO3).getDirectTemplateResourceId(),
                lessThan(resourcesByUnifiedResourceNo.get(UNIFIED_RESOURCE_NO2).getDirectTemplateResourceId()));
    }

    @Test
    public void processDelete() {
        var input = new TemplateInput()
                .withTemplateAction(TemplateAction.DELETE)
                .withDirectTemplateId(TEMPLATE_ID_OLD);

        var result = templateManagingTool.process(input);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getMessage()).isEqualTo(TEMPLATE_STRING + TEMPLATE_ID_OLD
                    + " has been successfully deleted");
            softly.assertThat(directTemplateRepository.get(List.of(TEMPLATE_ID_OLD))).hasSize(0);
            softly.assertThat(directTemplatePlaceRepository.getByTemplateId(TEMPLATE_ID_OLD)).hasSize(0);
            softly.assertThat(directTemplateResourceRepository.getByTemplateIds(List.of(TEMPLATE_ID_OLD))).hasSize(0);
        });
    }

    private void checkDirectTemplate(DirectTemplate templateToCheck,
                                     Long templateId, String formatName, DirectTemplateState state) {
        assertNotNull(templateToCheck);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(templateToCheck.getDirectTemplateId()).isEqualTo(templateId);
            softly.assertThat(templateToCheck.getFormatName()).isEqualTo(formatName);
            softly.assertThat(templateToCheck.getState()).isEqualTo(state);
        });
    }

    private void checkTemplatePlaces(List<TemplatePlace> placesToCheck,
                                     Long templateId, List<Long> placeIds) {
        assertThat(placesToCheck, hasSize(placeIds.size()));
        for (var templatePlace : placesToCheck) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(templatePlace.getTemplateId()).isEqualTo(templateId);
                softly.assertThat(templatePlace.getPlaceId()).isIn(placeIds);
            });
        }
    }

    private void checkDirectTemplateResource(Map<Long, DirectTemplateResource> resourcesByUnifiedResourceNo,
                                             Long unifiedResourceNo, Long templateId, Long resourceNo,
                                             Long unifiedTemplateResourceId,
                                             Set<DirectTemplateResourceOption> options,
                                             @Nullable Long directTemplateResourceId) {
        var resourceToCheck = resourcesByUnifiedResourceNo.get(unifiedResourceNo);
        assertThat(resourceToCheck, notNullValue());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(resourceToCheck.getDirectTemplateId()).isIn(templateId);
            softly.assertThat(resourceToCheck.getResourceNo()).isEqualTo(resourceNo);
            softly.assertThat(resourceToCheck.getUnifiedResourceNo()).isEqualTo(unifiedResourceNo);
            softly.assertThat(resourceToCheck.getUnifiedTemplateResourceId()).isEqualTo(unifiedTemplateResourceId);
            softly.assertThat(resourceToCheck.getOptions()).isEqualTo(options);
            if (directTemplateResourceId != null) {
                softly.assertThat(resourceToCheck.getDirectTemplateResourceId()).isEqualTo(directTemplateResourceId);
            }
        });
    }
}
