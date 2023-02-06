package ru.yandex.direct.jobs.placements;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.placements.model1.BlockSize;
import ru.yandex.direct.core.entity.placements.model1.IndoorBlock;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorBlock;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.Placement;
import ru.yandex.direct.core.entity.placements.model1.PlacementFormat;
import ru.yandex.direct.core.entity.placements.model1.PlacementPhoto;
import ru.yandex.direct.core.entity.placements.repository.PlacementRepository;
import ru.yandex.direct.core.entity.placements.service.UpdatePlacementService;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.jobs.placements.validation.PlacementTypeSpecificValidationProvider;
import ru.yandex.direct.juggler.JugglerStatus;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.yql.YqlConnection;
import ru.yandex.yql.YqlDataSource;
import ru.yandex.yql.YqlPreparedStatement;
import ru.yandex.yql.response.YqlResultBuilder;
import ru.yandex.yql.response.YqlResultSet;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.jobs.placements.PlacementsJobTestUtils.FIELDS_NUM;
import static ru.yandex.direct.jobs.placements.PlacementsJobTestUtils.FIELD_NAMES;
import static ru.yandex.direct.jobs.placements.PlacementsJobTestUtils.FIELD_TYPES;
import static ru.yandex.direct.jobs.placements.UpdatePlacementsJob.UPDATE_PLACEMENTS_OFFSET_KEY;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@JobsTest
@ExtendWith(SpringExtension.class)
class UpdatePlacementsJobExecuteTest {

    private static final Long OUTDOOR_PAGE_ID = 167L;

    @Autowired
    private PlacementRepository placementRepository;

    @Autowired
    private UpdatePlacementService updatePlacementService;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private PlacementTypeSpecificValidationProvider placementValidator;

    @Autowired
    private DirectConfig directConfig;

    private YqlPreparedStatement ytStatement;

    private UpdatePlacementsJob job;

    @Autowired
    private Steps steps;

    @BeforeEach
    public void setUp() throws SQLException {
        prepareJob();
        steps.placementSteps().clearPlacements();
        steps.placementSteps().addOutdoorOperator("rusoutdoor", "Russ Outdoor");

        steps.placementSteps().addPlacement(new OutdoorPlacement(OUTDOOR_PAGE_ID, "rusoutdoor.ru", "RusOutdoor",
                "rusoutdoor", "Russ Outdoor",
                false, false, true, emptyList(), List.of()));
        steps.placementSteps().addOperatorInternalName(OUTDOOR_PAGE_ID, "russ_outdoor");
        ppcPropertiesSupport.remove(UPDATE_PLACEMENTS_OFFSET_KEY.getName());
    }

    private void prepareJob() throws SQLException {
        YtProvider ytProvider = mock(YtProvider.class);
        YqlDataSource ytDataSource = mock(YqlDataSource.class);
        YqlConnection ytConnection = mock(YqlConnection.class);
        ytStatement = mock(YqlPreparedStatement.class);
        when(ytProvider.getYql(any(), any())).thenReturn(ytDataSource);
        when(ytDataSource.getConnection()).thenReturn(ytConnection);
        when(ytConnection.prepareStatement(any())).thenReturn(ytStatement);
        job = new UpdatePlacementsJob(updatePlacementService, ppcPropertiesSupport, placementValidator, directConfig,
                ytProvider);
    }

    @Test
    public void execute_success() throws Exception {
        String ytOutdoorBlocks =
                "[{\"Id\":12, \"Width\":\"1000.0\",\"Height\":\"500.0\",\"MinDuration\":7.5," +
                        "\"Resolution\":\"720x360\",\"Sizes\":[],"
                        + "\"GPS\":\"55.705534,37.563996\",\"FacilityType\":1,\"Direction\":\"180\",\"Address\":\"ул." +
                        " Мира\","
                        + "\"PhotoList\":[{\"formats\": [{\"height\":1000, \"width\":2500, \"path\":\"/some/path\"}]}],"
                        + "\"BlockCaption\":\"Test block caption of 12L\",\"IsHidden\":false}]";
        String ytOutdoorBlocks2 =
                "[{\"Id\":14, \"Width\":\"1000.0\",\"Height\":\"500.0\",\"MinDuration\":7.5," +
                        "\"Resolution\":\"720x360\",\"Sizes\":[],"
                        + "\"GPS\":\"55.705534,37.563996\",\"FacilityType\":1,\"Direction\":\"180\",\"Address\":\"ул." +
                        " Мира\","
                        + "\"PhotoList\":[{\"formats\": [{\"height\":1000, \"width\":2500, \"path\":\"/some/path\"}]}],"
                        + "\"BlockCaption\":\"Test block caption of 12L\",\"IsHidden\":false}]";
        String ytIndoorBlocks =
                "[{\"Id\":13, \"FacilityType\":\"2\",\"ZoneCategory\":\"1\",\"Resolution\":\"1280x200\",\"Sizes\":[],"
                        + "\"GPS\":\"56,38\",\"AspectRatio\":[],\"Address\":\"ул. Мира 2\","
                        + "\"PhotoList\":[{\"formats\": [{\"height\":120, \"width\":80, \"path\":\"/some/path\"}]}],"
                        + "\"BlockCaption\":\"Test block caption of 13L\",\"IsHidden\":true}]";
        YqlResultSet resultSet = initYtResult()
                .addRow("1", "unknown", "hightech.fm", "HighTech.FM", "false", "false", "false", "yastatic.net," +
                                "yandex-team.ru,yandex.az,yandex.by,yandex.com,yandex.ee,yandex.fr,yandex.kg,yandex" +
                                ".kz,yandex.lt",
                        "[]", "1", "hightech")
                .addRow("3", "outdoor", "rusoutdoor.ru", "RusOutdoor", "false", "false", "true", "",
                        ytOutdoorBlocks, "904", "rusoutdoor")
                .addRow("5", "outdoor", "rusoutdoor.ru", "RusOutdoor", "false", "false", "false", "", //no testing
                        // outdoor placement
                        ytOutdoorBlocks2, "904", "rusoutdoor")
                .addRow("7", "indoor", "rusindoor.ru", "RusIndoor", "false", "true", "false", "",
                        ytIndoorBlocks, "919", "rusindoor")
                .build();
        mockYtQueryResult(resultSet);

        job.execute();

        assertJugglerStatus(JugglerStatus.OK);
        assertUpdatePlacementsOffset(19L);

        OutdoorBlock outdoorBlock =
                new OutdoorBlock(3L, 12L, "Test block caption of 12L", LocalDateTime.now(), false, emptyList(), null,
                        "ул. Мира", null, "55.705534,37.563996", new BlockSize(720, 360), 1,
                        180, 1000.0, 500.0, 7.5, singletonList(new PlacementPhoto()
                        .withFormats(singletonList(new PlacementFormat()
                                .withWidth(2500)
                                .withHeight(1000)
                                .withPath("/some/path")))), false);
        OutdoorBlock outdoorBlock2 =
                new OutdoorBlock(5L, 14L, "Test block caption of 12L", LocalDateTime.now(), false, emptyList(), null,
                        "ул. Мира", null, "55.705534,37.563996", new BlockSize(720, 360), 1,
                        180, 1000.0, 500.0, 7.5, singletonList(new PlacementPhoto()
                        .withFormats(singletonList(new PlacementFormat()
                                .withWidth(2500)
                                .withHeight(1000)
                                .withPath("/some/path")))), false);
        IndoorBlock indoorBlock =
                new IndoorBlock(7L, 13L, LocalDateTime.now(), false, emptyList(), null,
                        "ул. Мира 2", null, "56,38", new BlockSize(1280, 200), 2, 1, emptyList(),
                        singletonList(new PlacementPhoto()
                                .withFormats(singletonList(new PlacementFormat()
                                        .withWidth(80)
                                        .withHeight(120)
                                        .withPath("/some/path")))), true);

        Collection<Placement> expectedPlacements = asList(
                new Placement<>(1L, null, "hightech.fm", "HighTech.FM", "hightech", null, false, false, false,
                        emptyList(), List.of()),
                new OutdoorPlacement(3L, "rusoutdoor.ru", "RusOutdoor", "rusoutdoor", "Russ Outdoor", false, false,
                        true,
                        singletonList(outdoorBlock), List.of()),
                new OutdoorPlacement(5L, "rusoutdoor.ru", "RusOutdoor", "rusoutdoor", "Russ Outdoor", false, false,
                        false,
                        singletonList(outdoorBlock2), List.of()),
                new IndoorPlacement(7L, "rusindoor.ru", "RusIndoor", "rusindoor", null, false, true, false,
                        singletonList(indoorBlock), List.of())
        );

        Collection<Placement> actualPlacements = placementRepository.getPlacements(mapList(expectedPlacements,
                Placement::getId)).values();

        Map<Long, Set<String>> placementsMirrors = placementRepository.getPlacementMirrors(mapList(expectedPlacements,
                Placement::getId));

        assertThat(placementsMirrors).containsKey(1L);
        assertThat(placementsMirrors.get(1L)).containsExactlyInAnyOrder("yastatic.net", "yandex-team.ru",
                "yandex.az",
                "yandex.by", "yandex.com", "yandex.ee", "yandex.fr", "yandex.kg", "yandex.kz",
                "yandex.lt");


        assertThat(actualPlacements)
                .describedAs("Wrong number of placements produced")
                .hasSize(expectedPlacements.size());
        assertThat(actualPlacements)
                .describedAs("Created placements is invalid")
                .is((matchedBy(beanDiffer(expectedPlacements)
                        .useCompareStrategy(allFieldsExcept(BeanFieldPath.newPath("0", "mirrors")).forFields(
                                BeanFieldPath.newPath("1", "blocks", "0", "lastChange"),
                                BeanFieldPath.newPath("2", "blocks", "0", "lastChange"),
                                BeanFieldPath.newPath("3", "blocks", "0", "lastChange"))
                                .useMatcher(approximatelyNow()))
                )));
    }

    @Test
    public void execute_newPlacementValidationError() throws Exception {
        long pageId = 3;
        long blockId = 12;

        String ytOutdoorBlocks = String.format("[%s]", ytValidOutdoorBlock(blockId));
        YqlResultBuilder resultSet = initYtResult();
        resultSet = addInvalidOutdoorPlacementToYtResult(resultSet, pageId, ytOutdoorBlocks);
        mockYtQueryResult(resultSet.build());

        job.execute();

        assertJugglerStatus(JugglerStatus.OK);
        assertUpdatePlacementsOffsetNotNull();

        Map<Long, Placement> actualPlacements = placementRepository.getPlacements(singletonList(pageId));

        assertThat(actualPlacements.values()).describedAs("Placement must not be created").isEmpty();
    }

    @Test
    public void execute_oldPlacementValidationError() throws Exception {
        long pageId = 3;
        long blockId = 12;

        OutdoorBlock block = outdoorBlockWithOneSize(pageId, blockId);
        OutdoorPlacement oldPlacement = steps.placementSteps().addOutdoorPlacement(pageId, singletonList(block));

        String ytOutdoorBlocks = String.format("[%s]", ytValidOutdoorBlock(blockId));
        YqlResultBuilder resultSet = initYtResult();
        resultSet = addInvalidOutdoorPlacementToYtResult(resultSet, pageId, ytOutdoorBlocks);
        mockYtQueryResult(resultSet.build());

        job.execute();

        assertJugglerStatus(JugglerStatus.OK);
        assertUpdatePlacementsOffsetNotNull();

        assertPlacementInDb(oldPlacement, emptyList());
    }

    @Test
    public void execute_validPlacementWithNoValidBlocks() throws Exception {
        long pageId = 3;
        long blockId = 12;

        String ytOutdoorBlocks = String.format("[%s]", ytInvalidOutdoorBlock(blockId));
        YqlResultBuilder resultSet = initYtResult();
        resultSet = addValidOutdoorPlacementToYtResult(resultSet, pageId, ytOutdoorBlocks);
        mockYtQueryResult(resultSet.build());

        job.execute();

        assertJugglerStatus(JugglerStatus.OK);
        assertUpdatePlacementsOffsetNotNull();

        Placement expectedPlacement = expectedOutdoorPlacement(pageId, emptyList());
        assertPlacementInDb(expectedPlacement, emptyList());
    }

    @Test
    public void execute_oneBlockInvalid_invalidBlockNotUpdatedOtherDataUpdated() throws Exception {
        long pageId = 3;
        long invalidBlockId = 12;
        long validBlockId = 13;

        OutdoorBlock invalidBlock = outdoorBlockWithOneSize(pageId, invalidBlockId);
        OutdoorBlock validBlock = outdoorBlockWithOneSize(pageId, validBlockId);
        steps.placementSteps().addOutdoorPlacement(pageId, asList(validBlock, invalidBlock));

        String ytOutdoorBlocks = String.format("[%s,%s]",
                ytInvalidOutdoorBlock(invalidBlockId), ytValidOutdoorBlock(validBlockId));
        YqlResultBuilder resultSet = initYtResult();
        resultSet = addValidOutdoorPlacementToYtResult(resultSet, pageId, ytOutdoorBlocks);
        mockYtQueryResult(resultSet.build());

        job.execute();

        assertJugglerStatus(JugglerStatus.OK);
        assertUpdatePlacementsOffsetNotNull();

        List<OutdoorBlock> expectedBlocks = asList(
                invalidBlock,
                expectedOutdoorBlock(pageId, validBlockId)
        );
        Placement expectedPlacement = expectedOutdoorPlacement(pageId, expectedBlocks);

        assertPlacementInDb(expectedPlacement, singletonList(1));
    }

    @Test
    public void execute_deletedPlacement_validationResultIgnored() throws Exception {
        long pageId = 3;
        long invalidBlockId = 12;
        long validBlockId = 13;

        OutdoorBlock invalidBlock = outdoorBlockWithOneSize(pageId, invalidBlockId);
        OutdoorBlock validBlock = outdoorBlockWithOneSize(pageId, validBlockId);
        steps.placementSteps().addOutdoorPlacement(pageId, asList(validBlock, invalidBlock));

        String ytOutdoorBlocks = String.format("[%s,%s]",
                ytInvalidOutdoorBlock(invalidBlockId), ytValidOutdoorBlock(validBlockId));
        YqlResultBuilder resultSet = initYtResult();
        resultSet = addDeletedOutdoorPlacementToYtResult(resultSet, pageId, ytOutdoorBlocks);
        mockYtQueryResult(resultSet.build());

        job.execute();

        assertJugglerStatus(JugglerStatus.OK);
        assertUpdatePlacementsOffsetNotNull();

        List<OutdoorBlock> expectedBlocks = asList(
                invalidBlock,
                expectedOutdoorBlock(pageId, validBlockId)
        );
        Placement expectedPlacement = expectedDeletedOutdoorPlacement(pageId, expectedBlocks);

        assertPlacementInDb(expectedPlacement, asList(0, 1));
    }

    @Test
    public void execute_validPlacementWithNoValidBlockResolution_ValidationError() throws Exception {
        long pageId = 3;
        long blockId = 12;

        String blockWithInvalidFfmpegResolution = ytOutdoorBlock(blockId, 10.0, "71x97");
        String invalidYtOutdoorBlocks = String.format("[%s]", blockWithInvalidFfmpegResolution);

        YqlResultBuilder resultSet = initYtResult();
        resultSet = addValidOutdoorPlacementToYtResult(resultSet, pageId, invalidYtOutdoorBlocks);
        mockYtQueryResult(resultSet.build());

        job.execute();

        assertJugglerStatus(JugglerStatus.OK);
        assertUpdatePlacementsOffsetNotNull();

        Placement expectedPlacement = expectedOutdoorPlacement(pageId, emptyList());
        assertPlacementInDb(expectedPlacement, emptyList());
    }

    private YqlResultBuilder initYtResult() {
        return YqlResultBuilder.builder(FIELDS_NUM)
                .names(FIELD_NAMES)
                .types(FIELD_TYPES);
    }

    private YqlResultBuilder addOutdoorPlacementToYtResult(YqlResultBuilder yqlResultBuilder, Long pageId,
                                                           String domain, String blocks) {
        return yqlResultBuilder.addRow(pageId.toString(), "outdoor", domain, "RusOutdoor", "false", "false", "false", "",
                blocks, "4", "rusoutdoor");
    }

    private YqlResultBuilder addDeletedOutdoorPlacementToYtResult(YqlResultBuilder yqlResultBuilder, Long pageId,
                                                                  String blocks) {
        return yqlResultBuilder.addRow(pageId.toString(), "outdoor", "rusoutdoor.ru", "RusOutdoor", "false", "true",
                "false", "", blocks, "4",
                "rusoutdoor");
    }

    private YqlResultBuilder addValidOutdoorPlacementToYtResult(YqlResultBuilder yqlResultBuilder, Long pageId,
                                                                String blocks) {
        return addOutdoorPlacementToYtResult(yqlResultBuilder, pageId, "rusoutdoor.ru", blocks);

    }

    private YqlResultBuilder addInvalidOutdoorPlacementToYtResult(YqlResultBuilder yqlResultBuilder, Long pageId,
                                                                  String blocks) {
        return addOutdoorPlacementToYtResult(yqlResultBuilder, pageId, "", blocks);
    }

    private void mockYtQueryResult(YqlResultSet resultSet) {
        try {
            when(ytStatement.executeQuery()).thenReturn(resultSet);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String ytOutdoorBlock(Long blockId, Double duration, String resolution) {
        return String.format(Locale.US, "{\"Id\":%d,\"MinDuration\":%f,\"Width\":\"1000.0\",\"Height\":\"500.0\"," +
                        "\"Resolution\":\"%s\",\"Sizes\":[],\"GPS\":\"55.705534,37.563996\",\"FacilityType\":1," +
                        "\"Direction\":\"180\",\"Address\":\"ул. Мира\"," +
                        "\"PhotoList\":[{\"formats\":[{\"height\":1000,\"width\":2500,\"path\":\"/some/path\"}]}]," +
                        "\"BlockCaption\":\"Test block caption of %d\",\"IsHidden\":false}", blockId, duration,
                resolution,
                blockId);
    }

    private String ytOutdoorBlock(Long blockId, Double duration) {
        return ytOutdoorBlock(blockId, duration, "720x360");
    }

    private String ytValidOutdoorBlock(Long blockId) {
        return ytOutdoorBlock(blockId, 10.0);
    }

    private String ytInvalidOutdoorBlock(Long blockId) {
        return ytOutdoorBlock(blockId, null);
    }

    private OutdoorPlacement expectedOutdoorPlacement(Long pageId, List<OutdoorBlock> blocks) {
        return new OutdoorPlacement(pageId, "rusoutdoor.ru", "RusOutdoor", "rusoutdoor", "Russ Outdoor", false,
                false, false, blocks, List.of());
    }

    private OutdoorPlacement expectedDeletedOutdoorPlacement(Long pageId, List<OutdoorBlock> blocks) {
        return new OutdoorPlacement(pageId, "rusoutdoor.ru", "RusOutdoor", "rusoutdoor", "Russ Outdoor", false, true,
                false, blocks, List.of());
    }

    private OutdoorBlock expectedOutdoorBlock(Long pageId, Long blockId) {
        return new OutdoorBlock(pageId, blockId, String.format("Test block caption of %d", blockId),
                LocalDateTime.now(), false, emptyList(), null,
                "ул. Мира", null, "55.705534,37.563996", new BlockSize(720, 360), 1,
                180, 1000.0, 500.0, 10.0, singletonList(new PlacementPhoto()
                .withFormats(singletonList(new PlacementFormat()
                        .withWidth(2500)
                        .withHeight(1000)
                        .withPath("/some/path")))), false);
    }

    private void assertJugglerStatus(JugglerStatus expectedJugglerStatus) {
        assertThat(job.getJugglerStatus())
                .describedAs("Wrong juggler status")
                .isEqualTo(expectedJugglerStatus);
    }

    private void assertUpdatePlacementsOffset(Long expectedUpdatePlacementsOffset) {
        assertThat(ppcPropertiesSupport.get(UPDATE_PLACEMENTS_OFFSET_KEY).get())
                .describedAs("Wrong max time value")
                .isEqualTo(expectedUpdatePlacementsOffset);
    }

    private void assertUpdatePlacementsOffsetNotNull() {
        assertThat(ppcPropertiesSupport.get(UPDATE_PLACEMENTS_OFFSET_KEY).get())
                .describedAs("Wrong max time value")
                .isNotNull();
    }

    private void assertPlacementInDb(Placement expectedPlacement, List<Integer> expectedBlockIndicesToBeUpdated) {
        Placement actualPlacement =
                placementRepository.getPlacements(singletonList(expectedPlacement.getId())).get(expectedPlacement.getId());
        assertThat(actualPlacement)
                .describedAs("Placement in database is invalid")
                .is(matchedBy(beanDiffer(expectedPlacement).useCompareStrategy(
                        allFieldsExcept(blocksFieldPath(expectedBlockIndicesToBeUpdated, "geoId"))
                                .forFields(blocksFieldPath(expectedBlockIndicesToBeUpdated, "lastChange"))
                                .useMatcher(approximatelyNow()))
                ));
    }

    private BeanFieldPath[] blocksFieldPath(List<Integer> blockIndices, String blockField) {
        return blockIndices.stream()
                .map(blockIndex -> BeanFieldPath.newPath("blocks", blockIndex.toString(), blockField))
                .toArray(BeanFieldPath[]::new);
    }
}
