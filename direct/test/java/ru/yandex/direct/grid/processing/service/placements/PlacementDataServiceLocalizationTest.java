package ru.yandex.direct.grid.processing.service.placements;

import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.placements.model1.PlacementType;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestPlacementRepository;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.PlacementSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.placement.GdIndoorPlacementBlock;
import ru.yandex.direct.grid.processing.model.placement.GdOutdoorPlacementBlock;
import ru.yandex.direct.grid.processing.model.placement.GdPlacement;
import ru.yandex.direct.grid.processing.model.placement.GdPlacementBlock;
import ru.yandex.direct.grid.processing.model.placement.GdPlacementContext;
import ru.yandex.direct.grid.processing.model.placement.GdPlacementFilter;
import ru.yandex.direct.grid.processing.service.placement.PlacementDataService;
import ru.yandex.direct.i18n.I18NBundle;
import ru.yandex.direct.i18n.Language;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlocks;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PlacementDataServiceLocalizationTest {

    @Autowired
    private PlacementSteps placementSteps;

    @Autowired
    private TestPlacementRepository placementRepository;

    @Autowired
    private PlacementDataService placementDataService;

    @Autowired
    private ClientSteps clientSteps;

    private static final Long OUTDOOR_BLOCK_ID_WITH_TRANSLATIONS = 182768L;
    private static final Long OUTDOOR_BLOCK_ID_WITHOUT_TRANSLATIONS = 182769L;
    private static final Long INDOOR_BLOCK_ID_WITH_TRANSLATIONS = 182770L;
    private static final Long INDOOR_BLOCK_ID_WITHOUT_TRANSLATIONS = 182771L;

    private final String address = "address";

    private final Map<Language, String> translations = Map.of(
            Language.RU, "ru",
            Language.EN, "en",
            Language.UK, "ua",
            Language.TR, "tr"
    );

    private ClientInfo defaultClient;

    @Before
    public void init() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        placementSteps.clearPlacements();
        Long pageIdOutdoor = placementRepository.getNextPageId();
        var outdoorBlocks = asList(
                outdoorBlockWithOneSize(pageIdOutdoor, OUTDOOR_BLOCK_ID_WITH_TRANSLATIONS)
                        .withAddress(address)
                        .withAddressTranslations(translations),
                outdoorBlockWithOneSize(pageIdOutdoor, OUTDOOR_BLOCK_ID_WITHOUT_TRANSLATIONS)
                        .withAddress(address)
                        .withAddressTranslations(null)
        );
        placementSteps.addPlacement(
                outdoorPlacementWithBlocks(pageIdOutdoor, outdoorBlocks)
        );
        Long pageIdIndoor = placementRepository.getNextPageId();
        var indoorBlocks = asList(
                indoorBlockWithOneSize(pageIdIndoor, INDOOR_BLOCK_ID_WITH_TRANSLATIONS)
                        .withAddress(address)
                        .withAddressTranslations(translations),
                indoorBlockWithOneSize(pageIdIndoor, INDOOR_BLOCK_ID_WITHOUT_TRANSLATIONS)
                        .withAddress(address)
                        .withAddressTranslations(null)
        );
        placementSteps.addPlacement(
                indoorPlacementWithBlocks(pageIdIndoor, indoorBlocks)
        );
        defaultClient = clientSteps.createDefaultClient();
    }

    @Test
    public void outdoorBlock_translationMapSet_UseEnFromTranslationMap() {
        var result = getPlacementsByType(PlacementType.OUTDOOR);
        GdOutdoorPlacementBlock outdoorBlock = getBlockByBlockId(result.getPlacements().get(0),
                OUTDOOR_BLOCK_ID_WITH_TRANSLATIONS);

        assertThat("Ожидался английский перевод адреса", outdoorBlock.getAddress(), is(translations.get(Language.EN)));
    }

    @Test
    public void outdoorBlock_noTranslationMap_UseAddress() {
        var result = getPlacementsByType(PlacementType.OUTDOOR);
        GdOutdoorPlacementBlock outdoorBlock = getBlockByBlockId(result.getPlacements().get(0),
                OUTDOOR_BLOCK_ID_WITHOUT_TRANSLATIONS);

        assertThat("Ожидалось значение в address", outdoorBlock.getAddress(), is(address));
    }

    @Test
    public void outdoorBlock_translationMapSet_UseAddressIfRu() {
        LocaleContextHolder.setLocale(I18NBundle.RU);
        var result = getPlacementsByType(PlacementType.OUTDOOR);
        GdOutdoorPlacementBlock outdoorBlock = getBlockByBlockId(result.getPlacements().get(0),
                OUTDOOR_BLOCK_ID_WITH_TRANSLATIONS);

        assertThat("Перевод с русского, ожидалось занчение в address", outdoorBlock.getAddress(), is(address));
    }

    @Test
    public void indoorBlock_translationMapSet_UseEnFromTranslationMap() {
        var result = getPlacementsByType(PlacementType.INDOOR);
        GdIndoorPlacementBlock outdoorBlock = getBlockByBlockId(result.getPlacements().get(0),
                INDOOR_BLOCK_ID_WITH_TRANSLATIONS);

        assertThat("Ожидался английский перевод адреса", outdoorBlock.getAddress(), is(translations.get(Language.EN)));
    }

    @Test
    public void indoorBlock_noTranslationMap_UseAddress() {
        var result = getPlacementsByType(PlacementType.INDOOR);
        GdIndoorPlacementBlock outdoorBlock = getBlockByBlockId(result.getPlacements().get(0),
                INDOOR_BLOCK_ID_WITHOUT_TRANSLATIONS);

        assertThat("Ожидалось значение в address", outdoorBlock.getAddress(), is(address));
    }

    @Test
    public void indoorBlock_translationMapSet_UseAddressIfRu() {
        LocaleContextHolder.setLocale(I18NBundle.RU);
        var result = getPlacementsByType(PlacementType.INDOOR);
        GdIndoorPlacementBlock outdoorBlock = getBlockByBlockId(result.getPlacements().get(0),
                INDOOR_BLOCK_ID_WITH_TRANSLATIONS);

        assertThat("Перевод с русского, ожидалось занчение в address", outdoorBlock.getAddress(), is(address));
    }

    @SuppressWarnings("ConstantConditions")
    private GdPlacementContext getPlacementsByType(PlacementType type) {
        return placementDataService.getPlacements(
                defaultClient.getClientId(),
                new GdPlacementFilter().withPlacementType(type)
        );
    }

    @SuppressWarnings("unchecked")
    private <T extends GdPlacementBlock> T getBlockByBlockId(GdPlacement gdPlacement, Long blockId) {
        return (T) gdPlacement.getBlocks()
                .stream()
                .filter(block -> block.getBlockId().equals(blockId))
                .findFirst()
                .orElse(null);
    }
}
