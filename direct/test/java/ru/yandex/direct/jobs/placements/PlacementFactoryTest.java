package ru.yandex.direct.jobs.placements;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.placements.model1.BlockSize;
import ru.yandex.direct.core.entity.placements.model1.IndoorBlock;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorBlock;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.Placement;
import ru.yandex.direct.core.entity.placements.model1.PlacementBlock;
import ru.yandex.direct.core.entity.placements.model1.PlacementPhoto;
import ru.yandex.direct.jobs.placements.container.YtBlockSize;
import ru.yandex.direct.jobs.placements.container.YtCommonBlock;
import ru.yandex.direct.jobs.placements.container.YtIndoorBlock;
import ru.yandex.direct.jobs.placements.container.YtOutdoorBlock;
import ru.yandex.yql.response.YqlResultBuilder;
import ru.yandex.yql.response.YqlResultSet;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestPlacements.defaultPhoto;
import static ru.yandex.direct.jobs.placements.PlacementsJobTestUtils.FIELDS_NUM;
import static ru.yandex.direct.jobs.placements.PlacementsJobTestUtils.FIELD_NAMES;
import static ru.yandex.direct.jobs.placements.PlacementsJobTestUtils.FIELD_TYPES;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.utils.JsonUtils.toJson;

class PlacementFactoryTest {

    private static final CompareStrategy PLACEMENT_STRATEGY =
            DefaultCompareStrategies.allFieldsExcept(newPath("blocks"));
    private static final CompareStrategy BLOCK_STRATEGY =
            DefaultCompareStrategies.allFields()
                    .forFields(newPath("lastChange")).useMatcher(approximatelyNow());

    @Test
    void createCommonPlacementWithUnknownType() throws SQLException {
        YtCommonBlock block = new YtCommonBlock()
                .withId(123L)
                .withBlockCaption("Test block caption of 123L")
                .withSizes(asList(
                        new YtBlockSize().withWidth(1280).withHeight(800),
                        new YtBlockSize().withWidth(1080).withHeight(720)));

        YqlResultSet resultSet = YqlResultBuilder.builder(FIELDS_NUM)
                .names(FIELD_NAMES)
                .types(FIELD_TYPES)
                .addRow("17", "unknown", "hightech.fm", "HighTech.FM",
                        "false", "true", "false", "", toJson(singletonList(block)), "1", "hightech")
                .build();

        resultSet.next();

        PlacementBlock expectedBlock = new PlacementBlock(17L, 123L, "Test block caption of 123L", now(), false,
                asList(new BlockSize(1280, 800), new BlockSize(1080, 720)));

        Placement expectedPlacement = new Placement<>(17L, null, "hightech.fm", "HighTech.FM",
                "hightech", null, false, true, false, singletonList(expectedBlock), List.of());

        Placement<?> actualPlacement = PlacementFactory.createPlacement(resultSet);
        assertThat(actualPlacement, beanDiffer(expectedPlacement).useCompareStrategy(PLACEMENT_STRATEGY));
        assertThat(actualPlacement.getBlocks().size(), equalTo(1));
        assertThat(actualPlacement.getBlocks().get(0),
                beanDiffer(expectedBlock).useCompareStrategy(BLOCK_STRATEGY));
    }

    @Test
    void createCommonPlacementWithUnknownTypeAndMirrors() throws SQLException {
        YtCommonBlock block = new YtCommonBlock()
                .withId(123L)
                .withBlockCaption("Test block caption of 123L")
                .withSizes(asList(
                        new YtBlockSize().withWidth(1280).withHeight(800),
                        new YtBlockSize().withWidth(1080).withHeight(720)));
        YqlResultSet resultSet = YqlResultBuilder.builder(FIELDS_NUM)
                .names(FIELD_NAMES)
                .types(FIELD_TYPES)
                .addRow("17", "unknown", "hightech.fm", "HighTech.FM",
                        "false", "true", "false",
                        "yastatic.net,yandex-team.ru,yandex.az,yandex.by,yandex.com,yandex.ee",
                        toJson(singletonList(block)), "1", "hightech")
                .build();

        resultSet.next();

        PlacementBlock expectedBlock = new PlacementBlock(17L, 123L, "Test block caption of 123L", now(), false,
                asList(new BlockSize(1280, 800), new BlockSize(1080, 720)));

        Placement expectedPlacement = new Placement<>(17L, null, "hightech.fm", "HighTech.FM",
                "hightech", null, false, true, false, singletonList(expectedBlock), List.of(
                "yastatic.net", "yandex-team.ru", "yandex.az", "yandex.by", "yandex.com", "yandex.ee"
        ));

        Placement<?> actualPlacement = PlacementFactory.createPlacement(resultSet);
        assertThat(actualPlacement, beanDiffer(expectedPlacement).useCompareStrategy(PLACEMENT_STRATEGY));
        assertThat(actualPlacement.getBlocks().size(), equalTo(1));
        assertThat(actualPlacement.getBlocks().get(0),
                beanDiffer(expectedBlock).useCompareStrategy(BLOCK_STRATEGY));
    }

    @Test
    void createIndoorPlacement() throws SQLException {
        List<PlacementPhoto> photos = singletonList(defaultPhoto());
        YtIndoorBlock block = new YtIndoorBlock()
                .withId(123L)
                .withSizes(asList(
                        new YtBlockSize().withWidth(1280).withHeight(800),
                        new YtBlockSize().withWidth(1080).withHeight(720)))
                .withAddress("address1")
                .withGps("34.493749,45.294893")
                .withResolution("800x600")
                .withFacilityType(17)
                .withZoneCategory(18)
                .withAspectRatio(asList(129, 1928))
                .withPhotos(photos)
                .withHidden(Boolean.FALSE);
        YqlResultSet resultSet = YqlResultBuilder.builder(FIELDS_NUM)
                .names(FIELD_NAMES)
                .types(FIELD_TYPES)
                .addRow("19", "indoor", "hightech.fm", "HighTech.FM",
                        "true", "false", "false", "", toJson(singletonList(block)), "1", "hightech")
                .build();

        resultSet.next();

        IndoorBlock expectedBlock = new IndoorBlock(19L, 123L, now(), false,
                asList(new BlockSize(1280, 800), new BlockSize(1080, 720)),
                null, "address1", null, "34.493749,45.294893", new BlockSize(800, 600), 17, 18,
                asList(129, 1928), photos, false);
        IndoorPlacement expectedPlacement = new IndoorPlacement(19L, "hightech.fm", "HighTech.FM",
                "hightech",
                null, true, false, false, singletonList(expectedBlock), List.of());

        Placement actualPlacement = PlacementFactory.createPlacement(resultSet);
        assertThat((IndoorPlacement) actualPlacement,
                beanDiffer(expectedPlacement).useCompareStrategy(PLACEMENT_STRATEGY));
        assertThat(actualPlacement.getBlocks().size(), equalTo(1));
        assertThat((IndoorBlock) actualPlacement.getBlocks().get(0),
                beanDiffer(expectedBlock).useCompareStrategy(BLOCK_STRATEGY));
    }

    @Test
    public void createOutdoorPlacement() throws SQLException {
        List<PlacementPhoto> photos = singletonList(defaultPhoto());
        YtOutdoorBlock block = new YtOutdoorBlock()
                .withId(123L)
                .withBlockCaption("Test block caption of 123L")
                .withSizes(asList(
                        new YtBlockSize().withWidth(1280).withHeight(800),
                        new YtBlockSize().withWidth(1080).withHeight(720)))
                .withAddress("address1")
                .withGps("34.493749,45.294893")
                .withResolution("800x600")
                .withFacilityType(17)
                .withDirection(198)
                .withWidth(1000.0)
                .withHeight(500.0)
                .withMinDuration(15.0)
                .withPhotos(photos)
                .withHidden(Boolean.FALSE);
        YqlResultSet resultSet = YqlResultBuilder.builder(FIELDS_NUM)
                .names(FIELD_NAMES)
                .types(FIELD_TYPES)
                .addRow("12", "outdoor", "hightech.fm", "HighTech.FM",
                        "true", "false", "true", "", toJson(singletonList(block)), "1", "hightech")
                .build();

        resultSet.next();

        OutdoorBlock expectedBlock = new OutdoorBlock(12L, 123L, "Test block caption of 123L", now(), false,
                asList(new BlockSize(1280, 800), new BlockSize(1080, 720)),
                null, "address1", null, "34.493749,45.294893", new BlockSize(800, 600), 17,
                198, 1000.0, 500.0, 15.0, photos, false);
        OutdoorPlacement expectedPlacement = new OutdoorPlacement(12L, "hightech.fm", "HighTech.FM",
                "hightech", null, true, false, true, singletonList(expectedBlock), List.of());

        Placement actualPlacement = PlacementFactory.createPlacement(resultSet);
        assertThat((OutdoorPlacement) actualPlacement,
                beanDiffer(expectedPlacement).useCompareStrategy(PLACEMENT_STRATEGY));
        assertThat(actualPlacement.getBlocks().size(), equalTo(1));
        assertThat((OutdoorBlock) actualPlacement.getBlocks().get(0),
                beanDiffer(expectedBlock).useCompareStrategy(BLOCK_STRATEGY));
    }
}
