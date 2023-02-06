package ru.yandex.market.billing.pp.storage;

import java.io.StringReader;

import javax.validation.ConstraintViolationException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.billing.pp.validation.PpValidationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

/**
 * Тесты для {@link PpJsonUtils}.
 *
 * @author vbudnev
 */
class PpJsonUtilsTest {

    @Test
    void test_loadPpDescriptions_when_emptyJson_should_throw() {
        final ConstraintViolationException ex = Assertions.assertThrows(ConstraintViolationException.class,
                () -> PpJsonUtils.loadPpDescriptions(new StringReader("{}")));
        Assertions.assertEquals("There must be at least 1 description", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_missingDescription_should_throw() {
        final ConstraintViolationException ex = Assertions.assertThrows(ConstraintViolationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{\"123\":{" +
                                "\"path\":\"somePath\"," +
                                "\"importance\":true" +
                                "}}"
                )));
        Assertions.assertEquals("Field \"description\" must be not null", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_tooShortDescription_should_throw() {
        final ConstraintViolationException ex = Assertions.assertThrows(ConstraintViolationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{\"123\":{" +
                                "\"path\":\"somePath\"," +
                                "\"importance\":true," +
                                "\"description\":\"\"" +
                                "}}"
                )));
        Assertions.assertEquals("Field \"description\" should be at least 1 character long", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_missingImportance_should_throw() {
        final ConstraintViolationException ex = Assertions.assertThrows(ConstraintViolationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{\"123\":{" +
                                "\"description\":\"someDesc\"," +
                                "\"path\":\"somePath\"" +
                                "}}"
                )));
        Assertions.assertEquals("Field \"importance\" must be not null", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_tooShortPath_should_throw() {
        final ConstraintViolationException ex = Assertions.assertThrows(ConstraintViolationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{\"123\":{" +
                                "\"description\":\"someDesc\"," +
                                "\"path\":\"\"," +
                                "\"importance\":true" +
                                "}}"
                )));
        Assertions.assertEquals("Field \"path\" should be at least 2 characters long", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_missingPath_should_throw() {
        final ConstraintViolationException ex = Assertions.assertThrows(ConstraintViolationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{\"123\":{" +
                                "\"description\":\"someDesc\"," +
                                "\"importance\":true" +
                                "}}"
                )));
        Assertions.assertEquals("Field \"path\" must be not null", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_validJson_should_buildExpected() {
        PpDescriptionsAll actual = PpJsonUtils.loadPpDescriptions(new StringReader(
                "{\"123\":{" +
                        "\"description\":\"someDesc\"," +
                        "\"path\":\"somePath\"," +
                        "\"validFor\": [\"CLICKS\"]," +
                        "\"ignoredFor\": [\"CPA_CLICKS\"]," +
                        "\"nonBillableFor\": [\"VENDOR_CLICKS\"]," +
                        "\"importance\":true" +
                        "}}"
        ));
        PpDescription ppDescription = new PpDescription();
        ppDescription.setDescription("someDesc");
        ppDescription.setImportance(true);
        ppDescription.setPath("somePath");
        ppDescription.setMarketTypeMarker(MarketTypeMarker.WHITE);
        ppDescription.setValidFor(ImmutableSet.of(PpDescription.LogTypeUsedIn.CLICKS));
        ppDescription.setIgnoredFor(ImmutableSet.of(PpDescription.LogTypeUsedIn.CPA_CLICKS));
        ppDescription.setNonBillableFor(ImmutableSet.of(PpDescription.LogTypeUsedIn.VENDOR_CLICKS));
        PpDescriptionsAll expected = new PpDescriptionsAll(ImmutableMap.of(123, ppDescription)).withEnrichedId();

        assertReflectionEquals(actual, expected);
        assertThat(ppDescription.getPpId(), equalTo(123));
    }

    @Test
    void test_loadPpDescriptions_when_hasExplicitMarketType_should_buildExpected() {
        PpDescriptionsAll actual = PpJsonUtils.loadPpDescriptions(new StringReader(
                "{\"123\":{" +
                        "\"description\":\"someDesc\"," +
                        "\"path\":\"somePath\"," +
                        "\"importance\":true," +
                        "\"validFor\": [\"CLICKS\"]," +
                        "\"ignoredFor\": [\"CPA_CLICKS\"]," +
                        "\"nonBillableFor\": [\"VENDOR_CLICKS\"]," +
                        "\"marketType\":\"BLUE\"" +
                        "}}"
        ));
        PpDescription ppDescription = new PpDescription();
        ppDescription.setDescription("someDesc");
        ppDescription.setImportance(true);
        ppDescription.setPath("somePath");
        ppDescription.setMarketTypeMarker(MarketTypeMarker.BLUE);
        ppDescription.setValidFor(ImmutableSet.of(PpDescription.LogTypeUsedIn.CLICKS));
        ppDescription.setIgnoredFor(ImmutableSet.of(PpDescription.LogTypeUsedIn.CPA_CLICKS));
        ppDescription.setNonBillableFor(ImmutableSet.of(PpDescription.LogTypeUsedIn.VENDOR_CLICKS));
        PpDescriptionsAll expected = new PpDescriptionsAll(ImmutableMap.of(123, ppDescription)).withEnrichedId();

        assertReflectionEquals(actual, expected);
        assertThat(ppDescription.getPpId(), equalTo(123));
    }

    @Test
    void test_loadPpDescriptions_when_isFreeNull_should_buildExpected() {
        PpDescriptionsAll actual = PpJsonUtils.loadPpDescriptions(new StringReader(
                "{\"123\":{" +
                        "\"description\":\"someDesc\"," +
                        "\"path\":\"somePath\"," +
                        "\"importance\":true," +
                        "\"validFor\": [\"CLICKS\"]," +
                        "\"ignoredFor\": [\"CPA_CLICKS\"]," +
                        "\"nonBillableFor\": [\"VENDOR_CLICKS\"]," +
                        "\"marketType\":\"BLUE\"" +
                        "}}"
        ));
        PpDescription ppDescription = new PpDescription();
        ppDescription.setDescription("someDesc");
        ppDescription.setImportance(true);
        ppDescription.setFree(null);
        ppDescription.setPath("somePath");
        ppDescription.setMarketTypeMarker(MarketTypeMarker.BLUE);
        ppDescription.setValidFor(ImmutableSet.of(PpDescription.LogTypeUsedIn.CLICKS));
        ppDescription.setIgnoredFor(ImmutableSet.of(PpDescription.LogTypeUsedIn.CPA_CLICKS));
        ppDescription.setNonBillableFor(ImmutableSet.of(PpDescription.LogTypeUsedIn.VENDOR_CLICKS));
        PpDescriptionsAll expected = new PpDescriptionsAll(ImmutableMap.of(123, ppDescription)).withEnrichedId();

        assertReflectionEquals(actual, expected);
        assertThat(ppDescription.getPpId(), equalTo(123));
    }

    @Test
    void test_loadPpDescriptions_when_isFreeNotNull_should_buildExpected() {
        PpDescriptionsAll actual = PpJsonUtils.loadPpDescriptions(new StringReader(
                "{\"123\":{" +
                        "\"description\":\"someDesc\"," +
                        "\"path\":\"somePath\"," +
                        "\"importance\":true," +
                        "\"isFree\":true," +
                        "\"validFor\": [\"CLICKS\"]," +
                        "\"ignoredFor\": [\"CPA_CLICKS\"]," +
                        "\"nonBillableFor\": [\"VENDOR_CLICKS\"]," +
                        "\"marketType\":\"BLUE\"" +
                        "}}"
        ));
        PpDescription ppDescription = new PpDescription();
        ppDescription.setDescription("someDesc");
        ppDescription.setImportance(true);
        ppDescription.setFree(true);
        ppDescription.setPath("somePath");
        ppDescription.setMarketTypeMarker(MarketTypeMarker.BLUE);
        ppDescription.setValidFor(ImmutableSet.of(PpDescription.LogTypeUsedIn.CLICKS));
        ppDescription.setIgnoredFor(ImmutableSet.of(PpDescription.LogTypeUsedIn.CPA_CLICKS));
        ppDescription.setNonBillableFor(ImmutableSet.of(PpDescription.LogTypeUsedIn.VENDOR_CLICKS));
        PpDescriptionsAll expected = new PpDescriptionsAll(ImmutableMap.of(123, ppDescription)).withEnrichedId();

        assertReflectionEquals(actual, expected);
        assertThat(ppDescription.getPpId(), equalTo(123));
        assertThat(ppDescription.getFree(), equalTo(true));
    }

    /**
     * Должны отрезаться /default окончания путей.
     */
    @Test
    void test_loadPpDescriptions_when_pathEndsWithDefault_should_trim() {
        PpDescriptionsAll pps = PpJsonUtils.loadPpDescriptions(new StringReader(
                "{" +
                        "\"123\":{" +
                        "\"description\":\"someDesc\"," +
                        "\"path\":\"root/nested/default\"," +
                        "\"validFor\": [\"CLICKS\"]," +
                        "\"importance\":true" +
                        "}," +
                        "\"124\":{" +
                        "\"description\":\"someDescr\"," +
                        "\"path\":\"root/nested/default/some_fake_end\"," +
                        "\"validFor\": [\"CLICKS\"]," +
                        "\"importance\":true" +
                        "}}"
        ));

        assertThat(pps.getPpDescriptionById().get(123).getPath(), is("root/nested"));
        assertThat(pps.getPpDescriptionById().get(124).getPath(), is("root/nested/default/some_fake_end"));
    }

    /**
     * Пустые ребра в {@link PpDescription#getPath()} неразрешены.
     */
    @Test
    void test_loadPpDescriptions_when_emptyEdge_should_throw() {
        final PpValidationException ex = Assertions.assertThrows(PpValidationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{" +
                                "\"124\":{" +
                                "\"description\":\"someDescr\"," +
                                "\"path\":\"root/nested//default/  /some_fake_end\"," +
                                "\"importance\":true" +
                                "}}"
                )));
        Assertions.assertEquals(
                "PP with id=124 contains empty edge for path=\"root/nested//default/  /some_fake_end\"",
                ex.getMessage()
        );
    }

    @Test
    void test_loadPpDescriptions_when_empty_all_for_should_throw() {
        final PpValidationException ex = Assertions.assertThrows(PpValidationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{" +
                                "\"124\":{" +
                                "\"description\":\"someDescr\"," +
                                "\"path\":\"root/nested/default/some_fake_end\"," +
                                "\"importance\":true" +
                                "}}"
                )));
        Assertions.assertEquals("PP with id = 124 has no entities set", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_empty_validFor_should_throw() {
        final ConstraintViolationException ex = Assertions.assertThrows(ConstraintViolationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{" +
                                "\"124\":{" +
                                "\"description\":\"someDescr\"," +
                                "\"path\":\"root/nested/default/some_fake_end\"," +
                                "\"importance\":true," +
                                "\"validFor\": []" +
                                "}}"
                )));
        Assertions.assertEquals("Field \"validFor\" must contain 1 to 6 entities", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_more_than_four_validFor_should_throw() {
        final PpValidationException ex = Assertions.assertThrows(PpValidationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{" +
                                "\"124\":{" +
                                "\"description\":\"someDescr\"," +
                                "\"path\":\"root/nested/default/some_fake_end\"," +
                                "\"importance\":true," +
                                "\"validFor\": [\"aaa\", \"bbb\", \"ccc\", \"ddd\", \"eee\"]" +
                                "}}"
                )));
        Assertions.assertEquals("PP with id = 124 has unknown entity in validFor field", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_unknown_validFor_should_throw() {
        final PpValidationException ex = Assertions.assertThrows(PpValidationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{" +
                                "\"124\":{" +
                                "\"description\":\"someDescr\"," +
                                "\"path\":\"root/nested/default/some_fake_end\"," +
                                "\"importance\":true," +
                                "\"validFor\": [\"aaa\"]" +
                                "}}"
                )));
        Assertions.assertEquals("PP with id = 124 has unknown entity in validFor field", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_same_unknown_entity_validFor_should_throw() {
        final PpValidationException ex = Assertions.assertThrows(PpValidationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{" +
                                "\"124\":{" +
                                "\"description\":\"someDescr\"," +
                                "\"path\":\"root/nested/default/some_fake_end\"," +
                                "\"importance\":true," +
                                "\"validFor\": [\"aaa\", \"aaa\", \"aaa\", \"aaa\", \"aaa\"]" +
                                "}}"
                )));
        Assertions.assertEquals("PP with id = 124 has unknown entity in validFor field", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_null_validFor_should_not_throw() {
        PpDescriptionsAll pps = PpJsonUtils.loadPpDescriptions(new StringReader(
                "{" +
                        "\"124\":{" +
                        "\"description\":\"someDescr\"," +
                        "\"path\":\"root/nested/default/some_fake_end\"," +
                        "\"importance\":true," +
                        "\"ignoredFor\": [\"CLICKS\"]" +
                        "}}"
        ));
        assertThat(pps.getPpDescriptionById().get(124).getValidFor(), is(nullValue()));
    }

    @Test
    void test_loadPpDescriptions_when_correct_validFor_should_not_throw() {
        PpDescriptionsAll pps = PpJsonUtils.loadPpDescriptions(new StringReader(
                "{" +
                        "\"124\":{" +
                        "\"description\":\"someDescr\"," +
                        "\"path\":\"root/nested/default/some_fake_end\"," +
                        "\"importance\":true," +
                        "\"validFor\": [\"SHOWS\", \"CLICKS\"]" +
                        "}}"
        ));
        assertThat(pps.getPpDescriptionById().get(124).getValidFor(),
                containsInAnyOrder(PpDescription.LogTypeUsedIn.SHOWS, PpDescription.LogTypeUsedIn.CLICKS));
    }

    @Test
    void test_loadPpDescriptions_when_empty_nonBillableFor_should_throw() {
        final ConstraintViolationException ex = Assertions.assertThrows(ConstraintViolationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{" +
                                "\"124\":{" +
                                "\"description\":\"someDescr\"," +
                                "\"path\":\"root/nested/default/some_fake_end\"," +
                                "\"importance\":true," +
                                "\"nonBillableFor\": []" +
                                "}}"
                )));
        Assertions.assertEquals("Field \"nonBillableFor\" must contain 1 to 4 entities", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_more_than_four_nonBillableFor_should_throw() {
        final PpValidationException ex = Assertions.assertThrows(PpValidationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{" +
                                "\"124\":{" +
                                "\"description\":\"someDescr\"," +
                                "\"path\":\"root/nested/default/some_fake_end\"," +
                                "\"importance\":true," +
                                "\"nonBillableFor\": [\"aaa\", \"bbb\", \"ccc\", \"ddd\", \"eee\"]" +
                                "}}"
                )));
        Assertions.assertEquals("PP with id = 124 has unknown entity in nonBillableFor field", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_unknown_nonBillableFor_should_throw() {
        final PpValidationException ex = Assertions.assertThrows(PpValidationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{" +
                                "\"124\":{" +
                                "\"description\":\"someDescr\"," +
                                "\"path\":\"root/nested/default/some_fake_end\"," +
                                "\"importance\":true," +
                                "\"nonBillableFor\": [\"aaa\"]" +
                                "}}"
                )));
        Assertions.assertEquals("PP with id = 124 has unknown entity in nonBillableFor field", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_same_unknown_entity_nonBillableFor_should_throw() {
        final PpValidationException ex = Assertions.assertThrows(PpValidationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{" +
                                "\"124\":{" +
                                "\"description\":\"someDescr\"," +
                                "\"path\":\"root/nested/default/some_fake_end\"," +
                                "\"importance\":true," +
                                "\"nonBillableFor\": [\"aaa\", \"aaa\", \"aaa\", \"aaa\", \"aaa\"]" +
                                "}}"
                )));
        Assertions.assertEquals("PP with id = 124 has unknown entity in nonBillableFor field", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_null_nonBillableFor_should_not_throw() {
        PpDescriptionsAll pps = PpJsonUtils.loadPpDescriptions(new StringReader(
                "{" +
                        "\"124\":{" +
                        "\"description\":\"someDescr\"," +
                        "\"path\":\"root/nested/default/some_fake_end\"," +
                        "\"importance\":true," +
                        "\"validFor\": [\"CLICKS\"]" +
                        "}}"
        ));
        assertThat(pps.getPpDescriptionById().get(124).getNonBillableFor(), is(nullValue()));
    }

    @Test
    void test_loadPpDescriptions_when_correct_nonBillableFor_should_not_throw() {
        PpDescriptionsAll pps = PpJsonUtils.loadPpDescriptions(new StringReader(
                "{" +
                        "\"124\":{" +
                        "\"description\":\"someDescr\"," +
                        "\"path\":\"root/nested/default/some_fake_end\"," +
                        "\"importance\":true," +
                        "\"nonBillableFor\": [\"SHOWS\", \"CLICKS\"]" +
                        "}}"
        ));
        assertThat(pps.getPpDescriptionById().get(124).getNonBillableFor(),
                containsInAnyOrder(PpDescription.LogTypeUsedIn.SHOWS, PpDescription.LogTypeUsedIn.CLICKS));
    }

    @Test
    void test_loadPpDescriptions_when_empty_ignoredFor_should_throw() {
        final ConstraintViolationException ex = Assertions.assertThrows(ConstraintViolationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{" +
                                "\"124\":{" +
                                "\"description\":\"someDescr\"," +
                                "\"path\":\"root/nested/default/some_fake_end\"," +
                                "\"importance\":true," +
                                "\"ignoredFor\": []" +
                                "}}"
                )));
        Assertions.assertEquals("Field \"ignoredFor\" must contain 1 to 4 entities", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_more_than_four_ignoredFor_should_throw() {
        final PpValidationException ex = Assertions.assertThrows(PpValidationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{" +
                                "\"124\":{" +
                                "\"description\":\"someDescr\"," +
                                "\"path\":\"root/nested/default/some_fake_end\"," +
                                "\"importance\":true," +
                                "\"ignoredFor\": [\"aaa\", \"bbb\", \"ccc\", \"ddd\", \"eee\"]" +
                                "}}"
                )));
        Assertions.assertEquals("PP with id = 124 has unknown entity in ignoredFor field", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_unknown_ignoredFor_should_throw() {
        final PpValidationException ex = Assertions.assertThrows(PpValidationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{" +
                                "\"124\":{" +
                                "\"description\":\"someDescr\"," +
                                "\"path\":\"root/nested/default/some_fake_end\"," +
                                "\"importance\":true," +
                                "\"ignoredFor\": [\"aaa\"]" +
                                "}}"
                )));
        Assertions.assertEquals("PP with id = 124 has unknown entity in ignoredFor field", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_same_unknown_entity_ignoredFor_should_throw() {
        final PpValidationException ex = Assertions.assertThrows(PpValidationException.class, () ->
                PpJsonUtils.loadPpDescriptions(new StringReader(
                        "{" +
                                "\"124\":{" +
                                "\"description\":\"someDescr\"," +
                                "\"path\":\"root/nested/default/some_fake_end\"," +
                                "\"importance\":true," +
                                "\"ignoredFor\": [\"aaa\", \"aaa\", \"aaa\", \"aaa\", \"aaa\"]" +
                                "}}"
                )));
        Assertions.assertEquals("PP with id = 124 has unknown entity in ignoredFor field", ex.getMessage());
    }

    @Test
    void test_loadPpDescriptions_when_null_ignoredFor_should_not_throw() {
        PpDescriptionsAll pps = PpJsonUtils.loadPpDescriptions(new StringReader(
                "{" +
                        "\"124\":{" +
                        "\"description\":\"someDescr\"," +
                        "\"path\":\"root/nested/default/some_fake_end\"," +
                        "\"importance\":true," +
                        "\"validFor\": [\"CLICKS\"]" +
                        "}}"
        ));
        assertThat(pps.getPpDescriptionById().get(124).getIgnoredFor(), is(nullValue()));
    }

    @Test
    void test_loadPpDescriptions_when_correct_ignoredFor_should_not_throw() {
        PpDescriptionsAll pps = PpJsonUtils.loadPpDescriptions(new StringReader(
                "{" +
                        "\"124\":{" +
                        "\"description\":\"someDescr\"," +
                        "\"path\":\"root/nested/default/some_fake_end\"," +
                        "\"importance\":true," +
                        "\"ignoredFor\": [\"SHOWS\", \"CLICKS\"]" +
                        "}}"
        ));
        assertThat(pps.getPpDescriptionById().get(124).getIgnoredFor(),
                containsInAnyOrder(PpDescription.LogTypeUsedIn.SHOWS, PpDescription.LogTypeUsedIn.CLICKS));
    }

    @Test
    void test_loadDictionaryDescriptions_when_missingCategories_should_throw() {
        final ConstraintViolationException ex = Assertions.assertThrows(ConstraintViolationException.class,
                () -> PpJsonUtils.loadDictionaryDescriptions(new StringReader("{}")));
        Assertions.assertEquals("Field \"categories\" should contain at least 1 element", ex.getMessage());
    }

    @Test
    void test_loadDictionaryDescriptions_when_validJson_should_buildExpected() {
        PpGroupsAll actual = PpJsonUtils.loadDictionaryDescriptions(new StringReader(
                "{\"categories\": [{" +
                        "\"code_category\": 3," +
                        "\"description\": \"поиск+\"," +
                        "\"pp_paths\": [\"desktop/direct\",\"desktop/maps\"]" +
                        "}]}"));
        PpGroupsAll expected = new PpGroupsAll();
        expected.setMarketTypeMarker(MarketTypeMarker.WHITE);
        PpGroup ppGroup = new PpGroup();
        ppGroup.setGroupId(3);
        ppGroup.setDescription("поиск+");
        ppGroup.setPpPaths(ImmutableList.of("desktop/direct", "desktop/maps"));
        expected.setGroups(ImmutableList.of(ppGroup));
        assertReflectionEquals(actual, expected);
    }

    @Test
    void test_loadDictionaryDescriptions_when_hasExpliticMarketType_should_buildExpected() {
        PpGroupsAll actual = PpJsonUtils.loadDictionaryDescriptions(new StringReader(
                "{\"categories\": [{" +
                        "\"code_category\": 3," +
                        "\"description\": \"поиск+\"," +
                        "\"pp_paths\": [\"desktop/direct\",\"desktop/maps\"]" +
                        "}]," +
                        "market_type:\"BLUE\"" +
                        "}"));
        PpGroupsAll expected = new PpGroupsAll();
        expected.setMarketTypeMarker(MarketTypeMarker.BLUE);
        PpGroup ppGroup = new PpGroup();
        ppGroup.setGroupId(3);
        ppGroup.setDescription("поиск+");
        ppGroup.setPpPaths(ImmutableList.of("desktop/direct", "desktop/maps"));
        expected.setGroups(ImmutableList.of(ppGroup));
        assertReflectionEquals(actual, expected);
    }

    @Test
    void test_loadDictionaryDescriptions_when_missingCode_should_throw() {
        final ConstraintViolationException ex = Assertions.assertThrows(ConstraintViolationException.class, () ->
                PpJsonUtils.loadDictionaryDescriptions(new StringReader(
                        "{\"categories\": [{" +
                                "\"description\": \"поиск+\"," +
                                "\"pp_paths\": [\"desktop/direct\",\"desktop/maps\"]" +
                                "}]}")));
        Assertions.assertEquals("Field \"code_category\" must be not null", ex.getMessage());
    }

    @Test
    void test_loadDictionaryDescriptions_when_missingDescription_should_throw() {
        final ConstraintViolationException ex = Assertions.assertThrows(ConstraintViolationException.class, () ->
                PpJsonUtils.loadDictionaryDescriptions(new StringReader(
                        "{\"categories\": [{" +
                                "\"code_category\": 3," +
                                "\"pp_paths\": [\"desktop/direct\",\"desktop/maps\"]" +
                                "}]}")));
        Assertions.assertEquals("Field \"description\" must be not null", ex.getMessage());
    }

    @Test
    void test_loadDictionaryDescriptions_when_missingPaths_should_throw() {
        final ConstraintViolationException ex = Assertions.assertThrows(ConstraintViolationException.class, () ->
                PpJsonUtils.loadDictionaryDescriptions(new StringReader(
                        "{\"categories\": [{" +
                                "\"code_category\": 3," +
                                "\"description\": \"поиск+\"" +
                                "}]}")));
        Assertions.assertEquals("Field \"pp_paths\" must be not null", ex.getMessage());
    }

    @Test
    void test_loadDictionaryDescriptions_when_emptyPaths_should_throw() {
        final ConstraintViolationException ex = Assertions.assertThrows(ConstraintViolationException.class, () ->
                PpJsonUtils.loadDictionaryDescriptions(new StringReader(
                        "{\"categories\": [{" +
                                "\"code_category\": 3," +
                                "\"description\": \"поиск+\"," +
                                "\"pp_paths\":[]" +
                                "}]}")));
        Assertions.assertEquals("Field \"pp_paths\" should contain at least 1 element", ex.getMessage());
    }

    @Test
    void test_loadDictionaryDescriptions_when_invalidCode_should_throw() {
        final ConstraintViolationException ex = Assertions.assertThrows(ConstraintViolationException.class, () ->
                PpJsonUtils.loadDictionaryDescriptions(new StringReader(
                        "{\"categories\": [{" +
                                "\"code_category\": -1," +
                                "\"description\": \"поиск+\"," +
                                "\"pp_paths\":[\"desktop/direct\",\"desktop/maps\"]" +
                                "}]}")));
        Assertions.assertEquals("Field \"code_category\" must be greater then 0", ex.getMessage());
    }

    private <T> void assertReflectionEquals(final T actual, final T expected) {
        final String actualText = ToStringBuilder.reflectionToString(actual, ToStringStyle.SIMPLE_STYLE);
        final String expectedText = ToStringBuilder.reflectionToString(expected, ToStringStyle.SIMPLE_STYLE);

        assertThat(actualText, equalTo(expectedText));
    }

}
