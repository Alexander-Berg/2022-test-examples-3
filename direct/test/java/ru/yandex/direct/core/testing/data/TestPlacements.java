package ru.yandex.direct.core.testing.data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import ru.yandex.direct.core.entity.adgroup.model.PageBlock;
import ru.yandex.direct.core.entity.placements.model1.BlockSize;
import ru.yandex.direct.core.entity.placements.model1.IndoorBlock;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorBlock;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.Placement;
import ru.yandex.direct.core.entity.placements.model1.PlacementBlock;
import ru.yandex.direct.core.entity.placements.model1.PlacementFormat;
import ru.yandex.direct.core.entity.placements.model1.PlacementPhoto;
import ru.yandex.direct.i18n.Language;

import static com.google.common.base.Preconditions.checkArgument;
import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.regions.Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class TestPlacements {

    /*
        Данные в пейджах должны отличаться, так как они используются для проверки обновления
     */
    public static Placement<PlacementBlock> emptyCommonYandexPlacement(Long id) {
        return new Placement<>(id, null, "www.yandex.ru", "Yandex", "yandex-login-1", null, true, false,
                false, emptyList(), List.of());
    }

    public static Placement<PlacementBlock> emptyCommonYandexPlacementWithMirrors(Long id) {
        return new Placement<>(id, null, "www.yandex.ru", "Yandex", "yandex-login-1", null, true, false,
                false, emptyList(), List.of("mirror1.ru", "mirror2.com"));
    }

    public static Placement<PlacementBlock> commonYandexPlacementWithBlocks(Long id, List<PlacementBlock> blocks) {
        return new Placement<>(id, null, "www.yandex.ru2", "Yandex2", "yandex-login-1", null, true, false, false,
                blocks, List.of());
    }

    public static Placement<PlacementBlock> commonYandexPlacementWithBlock(Long id, PlacementBlock blocks) {
        return commonYandexPlacementWithBlocks(id, singletonList(blocks));
    }

    public static OutdoorPlacement emptyOutdoorPlacement(Long id) {
        return new OutdoorPlacement(id, "rusoutdoor.ru", "RusOutdoor", "rusoutdoor-login-1",
                null, false, false, false, emptyList(), List.of());
    }

    public static OutdoorPlacement emptyDeletedOutdoorPlacement(Long id) {
        return new OutdoorPlacement(id, "rusoutdoor2.ru", "RusOutdoor2", "rusoutdoor-login-2",
                null, false, true, false, emptyList(), List.of());
    }

    public static OutdoorPlacement outdoorPlacementWithBlocks(Long id, List<OutdoorBlock> blocks) {
        return new OutdoorPlacement(id, "rusoutdoor3.ru", "RusOutdoor3", "rusoutdoor-login-3",
                null, true, false, false, blocks, List.of());
    }

    public static OutdoorPlacement outdoorPlacementWithBlock(Long id, OutdoorBlock block) {
        return outdoorPlacementWithBlocks(id, singletonList(block));
    }

    public static OutdoorPlacement outdoorPlacementWithDefaultBlock(Long id, Long blockId) {
        return new OutdoorPlacement(id, "rusoutdoor4.ru", "RusOutdoor4", "rusoutdoor-login-4",
                null, true, false, false,
                singletonList(outdoorBlockWithOneSize(id, blockId)), List.of());
    }

    public static OutdoorPlacement outdoorTestingPlacement(Long id, OutdoorBlock block) {
        return new OutdoorPlacement(id, "rusoutdoor5.ru", "RusOutdoor5", "rusoutdoor-login-5",
                null, true, false, true, singletonList(block), List.of());
    }

    public static IndoorPlacement emptyIndoorPlacement(Long id) {
        return new IndoorPlacement(id, "rusindoor.ru3", "RusIndoor3", "rusindoor-login-3", null, false, false,
                false, emptyList(), List.of());
    }

    public static IndoorPlacement emptyDeletedIndoorPlacement(Long id) {
        return new IndoorPlacement(id, "rusindoor.ru4", "RusIndoor4", "rusindoor-login-4", null, false, true,
                false, emptyList(), List.of());
    }

    public static IndoorPlacement indoorPlacementWithBlocks(Long id, List<IndoorBlock> blocks) {
        return new IndoorPlacement(id, "rusindoor.ru5", "RusIndoor5", "rusindoor-login-5", null, true, false, false,
                blocks, List.of());
    }

    public static IndoorPlacement indoorPlacementWithBlock(Long id, IndoorBlock block) {
        return indoorPlacementWithBlocks(id, singletonList(block));
    }

    public static IndoorPlacement indoorPlacementWithDefaultBlock(Long id, Long blockId) {
        return new IndoorPlacement(id, "rusindoor.ru6", "RusIndoor6", "rusindoor-login-6", null, true, false,
                false, singletonList(indoorBlockWithOneSize(id, blockId)), List.of());
    }

    /*
        Данные в блоках должны отличаться, так как они используются для проверки обновления
     */

    public static PlacementBlock commonBlockWithEmptySizes(Long pageId, Long blockId) {
        return new PlacementBlock(pageId, blockId, "Common block caption", defaultLastChange(), false, emptyList());
    }

    public static PlacementBlock commonBlockWithOneSize(Long pageId, Long blockId) {
        return commonBlockWithOneSize(pageId, blockId, defaultLastChange());
    }

    public static PlacementBlock commonBlockWithOneSizeWithoutGeo(Long pageId, Long blockId) {
        return commonBlockWithOneSize(pageId, blockId, defaultLastChange());
    }

    public static PlacementBlock commonBlockWithOneSize(Long pageId, Long blockId, LocalDateTime lastChange) {
        return new PlacementBlock(pageId, blockId, "Common block caption", lastChange, false, oneBlockSize());
    }

    public static PlacementBlock commonBlockWithOneSize300x300(Long pageId, Long blockId) {
        return new PlacementBlock(pageId, blockId, "Common block caption 300x300", defaultLastChange(), false,
                singletonList(new BlockSize(300, 300)));
    }

    public static PlacementBlock commonBlockWithTwoSizes(Long pageId, Long blockId) {
        return new PlacementBlock(pageId, blockId, "Common block caption 2", defaultLastChange(), false,
                twoBlockSizes());
    }

    public static PlacementBlock commonDeletedBlock(Long pageId, Long blockId) {
        return new PlacementBlock(pageId, blockId, "Common deleted block caption", defaultLastChange(), true,
                twoBlockSizes());
    }

    public static OutdoorBlock outdoorBlockWithOneSize(Long pageId, Long blockId) {
        return outdoorBlockWithOneSize(pageId, blockId, defaultLastChange());
    }

    public static OutdoorBlock outdoorBlockWithOneSize(Long pageId, Long blockId, LocalDateTime lastChange) {
        return outdoorBlockWithOneSize(pageId, blockId, lastChange, "55.709029, 37.392839");
    }

    public static OutdoorBlock outdoorBlockWithOneSize(Long pageId, Long blockId, BlockSize resolution,
                                                       Double duration) {
        return outdoorBlockWithOneSize(pageId, blockId, "Outdoor block caption", defaultLastChange(), "55.709129, 37" +
                        ".393839",
                MOSCOW_REGION_ID, resolution, duration);
    }

    public static OutdoorBlock outdoorBlockWithOneSize(Long pageId, Long blockId, Long geoId) {
        return outdoorBlockWithOneSize(pageId, blockId, "Outdoor block caption", defaultLastChange(), "55.709129, 37" +
                        ".393839",
                geoId, new BlockSize(1280, 800), 7.5);
    }

    public static OutdoorBlock outdoorBlockWithFacilityType(Long pageId, Long blockId, Integer facilityType) {
        return new OutdoorBlock(pageId, blockId, "Outdoor block caption", defaultLastChange(), false, oneBlockSize(),
                MOSCOW_REGION_ID, "address1", defaultAddressTranslations(), "55.709329, 37.392879",
                new BlockSize(1280, 800),
                facilityType, 45, 1000.0, 500.0, 5.0, emptyList(), false);
    }

    public static OutdoorBlock outdoorBlockWithOneSize(Long pageId, Long blockId, String blockCaption) {
        return outdoorBlockWithOneSize(pageId, blockId, blockCaption, defaultLastChange(), "55.709129, 37.393839",
                MOSCOW_REGION_ID, new BlockSize(1280, 800), 7.5);
    }

    public static OutdoorBlock outdoorBlockWithOneSize(Long pageId, Long blockId, LocalDateTime lastChange,
                                                       String coordinates) {
        return outdoorBlockWithOneSize(pageId, blockId, "Outdoor block caption", lastChange, coordinates,
                MOSCOW_REGION_ID,
                new BlockSize(1280, 800), 7.5);
    }

    public static OutdoorBlock outdoorBlockWithOneSize(Long pageId, Long blockId, String blockCaption,
                                                       LocalDateTime lastChange, String coordinates,
                                                       Long geoId, BlockSize resolution, Double duration) {
        return new OutdoorBlock(pageId, blockId, blockCaption, lastChange, false, oneBlockSize(),
                geoId, "address1", defaultAddressTranslations(), coordinates, resolution, 1,
                45, 1080.0, 720.0, duration, singletonList(defaultPhoto()), false);
    }

    public static OutdoorBlock outdoorBlockWithOneSizeWithoutGeo(Long pageId, Long blockId) {
        return outdoorBlockWithOneSize(pageId, blockId, (Long) null);
    }

    public static OutdoorBlock outdoorBlockWithOneSizeWithoutGeo(Long pageId, Long blockId, LocalDateTime lastChange) {
        return new OutdoorBlock(pageId, blockId, "Outdoor block caption", lastChange, false, oneBlockSize(),
                null, "address1", defaultAddressTranslations(), "55.719498, 37.382397", new BlockSize(1280, 800),
                1, 0, 300.0, 100.0, 8.5, singletonList(defaultPhoto()),
                false);
    }

    public static OutdoorBlock outdoorBlockWithOneSize2(Long pageId, Long blockId) {
        return new OutdoorBlock(pageId, blockId, "Outdoor block caption", defaultLastChange(), false, oneBlockSize(),
                SAINT_PETERSBURG_REGION_ID, "address3", defaultAddressTranslations(), "55.719029, 37.382839",
                new BlockSize(1440, 720), 2, 120, 999.0, 111.0, 15.0,
                singletonList(defaultPhoto()), false);
    }

    public static OutdoorBlock outdoorBlockWithTwoSizes(Long pageId, Long blockId) {
        return outdoorBlockWithTwoSizes(pageId, blockId, MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID);
    }

    public static OutdoorBlock outdoorBlockWithTwoSizes(Long pageId, Long blockId, Long geoId) {
        return new OutdoorBlock(pageId, blockId, "Outdoor block caption 2", defaultLastChange(), false, twoBlockSizes(),
                geoId, "address3", defaultAddressTranslations(), "55.705534, 37.563996", new BlockSize(1080, 720), 3,
                180, 1000.0, 100.0, 11.1, singletonList(defaultPhoto()), false);
    }

    public static OutdoorBlock outdoorBlockWithTwoSizesWithoutGeo(Long pageId, Long blockId) {
        return outdoorBlockWithTwoSizes(pageId, blockId, null);
    }

    public static OutdoorBlock outdoorDeletedBlock(Long pageId, Long blockId) {
        return outdoorDeletedBlock(pageId, blockId, "Outdoor block caption");
    }

    public static OutdoorBlock outdoorDeletedBlock(Long pageId, Long blockId, String blockCaption) {
        return new OutdoorBlock(pageId, blockId, blockCaption, defaultLastChange(), true, twoBlockSizes(),
                SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID,
                "address4", defaultAddressTranslations(), "55.701210, 37.293028", new BlockSize(800, 600), 4,
                120, 2048.0, 1800.0, 30.15, singletonList(defaultPhoto()), false);
    }

    public static OutdoorBlock outdoorDeletedBlockWithoutGeo(Long pageId, Long blockId) {
        return new OutdoorBlock(pageId, blockId, "Outdoor block caption without geo", defaultLastChange(), true,
                twoBlockSizes(), null, "address4", defaultAddressTranslations(), "55.701210, 37.293028",
                new BlockSize(800, 600), 4, 120, 2048.0, 1800.0, 30.15,
                singletonList(defaultPhoto()), false);
    }

    public static OutdoorBlock outdoorBlockWithCoordinates(Long pageId, Long blockId, String coordinates) {
        return new OutdoorBlock(pageId, blockId, "Outdoor block caption", defaultLastChange(), false, oneBlockSize(),
                SAINT_PETERSBURG_REGION_ID, "address3", defaultAddressTranslations(), coordinates,
                new BlockSize(1440, 720), 2, 120, 999.0, 111.0, 15.0, singletonList(defaultPhoto()), false);
    }

    public static OutdoorBlock outdoorBlockWithAddressTranslations(Long pageId, Long blockId,
                                                                   Map<Language, String> addressTranslations) {
        return new OutdoorBlock(pageId, blockId, "Outdoor block caption", defaultLastChange(), false, oneBlockSize(),
                SAINT_PETERSBURG_REGION_ID, "address3", addressTranslations, "55.701210, 37.293028",
                new BlockSize(1440, 720), 2, 120, 999.0, 111.0, 15.0, singletonList(defaultPhoto()), false);
    }

    public static IndoorBlock indoorBlockWithOneSize(Long pageId, Long blockId) {
        return new IndoorBlock(pageId, blockId, defaultLastChange(), false, oneBlockSize(), MOSCOW_REGION_ID,
                "address1", defaultAddressTranslations(), "55.709029, 37.392839", new BlockSize(1280, 800),
                1, 2, asList(16, 9), singletonList(defaultPhoto()), false);
    }

    public static IndoorBlock indoorBlockWithOneSize(Long pageId, Long blockId, LocalDateTime lastChange) {
        return new IndoorBlock(pageId, blockId, lastChange, false, oneBlockSize(),
                MOSCOW_REGION_ID, "address1", defaultAddressTranslations(), "55.709029, 37.392839",
                new BlockSize(1280, 800), 1, 2, asList(16, 9),
                singletonList(defaultPhoto()), false);
    }

    public static IndoorBlock indoorBlockWithFacilityType(Long pageId, Long blockId, Integer facilityType) {
        return new IndoorBlock(pageId, blockId, defaultLastChange(), false, oneBlockSize(), MOSCOW_REGION_ID,
                "address1", defaultAddressTranslations(), "55.709329, 37.392859", new BlockSize(1280, 800),
                facilityType, 2, asList(9, 16), singletonList(defaultPhoto()), false);
    }

    public static IndoorBlock indoorBlockWithZoneCategory(Long pageId, Long blockId, Integer zoneCategory) {
        return new IndoorBlock(pageId, blockId, defaultLastChange(), false, oneBlockSize(), MOSCOW_REGION_ID,
                "address1", defaultAddressTranslations(), "55.709349, 37.392889", new BlockSize(1280, 800),
                1, zoneCategory, asList(17, 10), singletonList(defaultPhoto()), false);
    }

    public static IndoorBlock indoorBlockWithTwoSizes(Long pageId, Long blockId) {
        return new IndoorBlock(pageId, blockId, defaultLastChange(), false, twoBlockSizes(),
                SAINT_PETERSBURG_REGION_ID, "address2", defaultAddressTranslations(), "55.705534, 37.563996",
                new BlockSize(1080, 720), 3, 4, asList(3, 1), singletonList(defaultPhoto()), false);
    }

    public static IndoorBlock indoorDeletedBlock(Long pageId, Long blockId) {
        return new IndoorBlock(pageId, blockId, defaultLastChange(), true, twoBlockSizes(),
                MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID,
                "address3", defaultAddressTranslations(), "55.701210, 37.293028", new BlockSize(800, 600),
                4, 8, asList(12, 6), singletonList(defaultPhoto()), false);
    }

    public static Placement<PlacementBlock> gradusnikPlacement(Long id) {
        return emptyPlacement("gradusnik.ru", id);
    }

    public static Placement<PlacementBlock> emptyPlacement(String domain, Long id) {
        return emptyPlacement(domain, id, false);
    }

    public static Placement<PlacementBlock> emptyPlacement(String domain, Long id, boolean isDeleted) {
        return new Placement<>(id, null, domain, "gradusnik", "gradusnik-login-1", null, false, isDeleted,
                false, emptyList(), List.of("mirror_" + id, "mirror2_" + id));
    }

    public static <B extends PlacementBlock> List<PageBlock> placementToPageBlocks(Placement<B> placement) {
        List<B> blocks = placement == null ? null : placement.getBlocks();
        return blocks == null ?
                null :
                mapList(blocks,
                        block -> new PageBlock()
                                .withPageId(block.getPageId())
                                .withImpId(block.getBlockId()));
    }

    public static OutdoorPlacement copyOutdoorPlacementForceSetGeo(
            OutdoorPlacement outdoorPlacement, Long... geoIds) {
        checkArgument(outdoorPlacement.getBlocks().size() == geoIds.length);
        List<OutdoorBlock> outdoorBlocksCopy = new ArrayList<>();

        for (int i = 0; i < outdoorPlacement.getBlocks().size(); i++) {
            var b = outdoorPlacement.getBlocks().get(i);
            var blockCopy = new OutdoorBlock(b.getPageId(), b.getBlockId(), b.getBlockCaption(),
                    b.getLastChange(),
                    b.isDeleted(), b.getSizes(), geoIds[i], b.getAddress(), b.getAddressTranslations(),
                    b.getCoordinates(), b.getResolution(), b.getFacilityType(), b.getDirection(), b.getWidth(),
                    b.getHeight(), b.getDuration(), b.getPhotos(), b.getHidden());
            outdoorBlocksCopy.add(blockCopy);
        }
        return outdoorPlacement.replaceBlocks(outdoorBlocksCopy);
    }

    public static IndoorPlacement copyIndoorPlacementForceSetGeo(
            IndoorPlacement indoorPlacement, Long... geoIds) {
        checkArgument(indoorPlacement.getBlocks().size() == geoIds.length);
        List<IndoorBlock> indoorBlocksCopy = new ArrayList<>();

        for (int i = 0; i < indoorPlacement.getBlocks().size(); i++) {
            var b = indoorPlacement.getBlocks().get(i);
            var blockCopy = new IndoorBlock(b.getPageId(), b.getBlockId(),
                    b.getLastChange(), b.isDeleted(), b.getSizes(), geoIds[i], b.getAddress(),
                    b.getAddressTranslations(),
                    b.getCoordinates(), b.getResolution(), b.getFacilityType(),
                    b.getZoneCategory(), b.getAspectRatio(), b.getPhotos(), b.getHidden());
            indoorBlocksCopy.add(blockCopy);
        }
        return indoorPlacement.replaceBlocks(indoorBlocksCopy);
    }

    private static List<BlockSize> oneBlockSize() {
        return singletonList(new BlockSize(350, 350));
    }

    private static List<BlockSize> twoBlockSizes() {
        return asList(new BlockSize(240, 400), new BlockSize(728, 90));
    }

    private static LocalDateTime defaultLastChange() {
        return now().truncatedTo(ChronoUnit.SECONDS);
    }

    public static PlacementPhoto defaultPhoto() {
        return new PlacementPhoto().withFormats(asList(
                new PlacementFormat()
                        .withWidth(1234)
                        .withHeight(345)
                        .withPath("/operator1/pic.jpg"),
                new PlacementFormat()
                        .withWidth(2000)
                        .withHeight(500)
                        .withPath("/operator1/pic2.jpg")));
    }

    private static Map<Language, String> defaultAddressTranslations() {
        return ImmutableMap.of(
                Language.RU, "address ru",
                Language.EN, "address en",
                Language.UK, "address ua",
                Language.TR, "address tr"
        );
    }
}
