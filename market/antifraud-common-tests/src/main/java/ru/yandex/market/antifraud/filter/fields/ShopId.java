package ru.yandex.market.antifraud.filter.fields;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ru.yandex.market.antifraud.filter.RndUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Created by entarrion on 22.01.15.
 */
public class ShopId {
    private static final ImmutableMap<Integer, Integer> SHOPS_WITH_DELIVERY_TO_COUNTRY;
    private static final ImmutableMap<Integer, Integer> SHOPS_WITH_DELIVERY_TO_REGION;
    private static final ImmutableMap<Integer, Integer> SHOPS_FOR_SNG_DELIVERY;
    private static final ImmutableMap<Integer, Integer> SHOPS_WITHOUT_DELIVERY_TO_REGION;
    private static final ImmutableMap<Integer, Integer> SHOPS_FOREIGN_DELIVERY;
    public static List<Integer> TEST_SHOPS = ImmutableList.of(774, 64407, 81832, 82036, 59048, 61494);

    static {
        //select shop_id from shops where shop_id is NOT NULL and array_contains(regions,225) limit 500
        Map<Integer, Integer> shopGeoId = createMapFromShopIdsWithGeoId(213,
                405014, 404731, 403947, 403856, 403651, 402423, 402187, 401642, 399525, 399485, 399458, 399397, 399387,
                399343, 399338, 399294, 399225, 399223, 399214, 399206, 399173, 399170, 399166, 399095, 399070, 399069,
                399065, 399062, 399045, 399033, 399005, 398968, 398963, 398953, 398936, 398931, 398885, 398879, 398872,
                398865, 398850, 398823, 398803, 398750, 398723, 398722, 398716, 398685, 398683, 398659, 398637, 398625,
                398578, 398547, 398542, 398514, 398508, 398500, 398484, 398443, 398421, 398419, 398395, 398390,
                398386, 398376, 398374, 398364, 398359, 398357, 398336, 398327, 398296, 398294, 398256, 398249, 398247,
                398242, 398202, 398175, 398159, 398145, 398141, 398134, 398128, 398123, 398121, 398073, 398054, 398011,
                397987, 397986, 397965, 397950, 397939, 397906, 397817, 397811, 397795, 397786, 397785, 397779, 397776,
                397772, 397760, 397700, 397698, 397674, 397668, 397651, 397613, 397603, 397537, 397493, 397486, 397465,
                397456, 397447, 397424, 397420, 397405, 397360, 397338, 397336, 397307, 397286, 397284, 397245, 397217,
                397216, 397214, 397213, 397209, 397180, 397154, 397125, 397119, 397092, 396989, 396971, 396953, 396900,
                396878, 396871, 396806, 396804, 396799, 396780, 396776, 396771, 396766, 396750, 396736, 396733, 396697,
                396677, 396668, 396665, 396633, 396604, 396583, 396558, 396524, 396522, 396521, 396504, 396476, 396469,
                396466, 396422, 396407, 396396, 396379, 396361, 396356, 396354, 396324, 396306, 396303, 396271, 396248,
                396221, 396180, 396164, 396153, 396099, 396089, 396046, 396027, 396022, 396018, 395952, 395912, 395900,
                395847, 395831, 395753, 395747, 395731, 395726, 395701, 395700, 395643, 395590, 395566, 395551, 395547,
                395533, 395494, 395468, 395464, 395452, 395430, 395377, 395375, 395344, 395341, 395328, 395319, 395296,
                395295, 395247, 395246, 395226, 395211, 395170, 395158, 395151, 395116, 395108, 395101, 395094, 395077,
                395054, 394995, 394972, 394950, 394925, 394916, 394889, 394878, 394875, 394836, 394823, 394809, 394785,
                394782, 394747, 394746, 394742, 394738, 394735, 394734, 394682, 394676, 394665, 394661, 394646, 394643,
                394632, 394623, 394622, 394615, 394598, 394585, 394574, 394570, 394567, 394563, 394548, 394532, 394529,
                394528, 394527, 394521, 394519, 394490, 394484, 394458, 394434, 394400, 394378, 394369, 394363, 394350,
                394339, 394320, 394311, 394300, 394270, 394266, 394259, 394242, 394238, 394224, 394219, 394208, 394183,
                394170, 394140, 394136, 394127, 394116, 394095, 394070, 394062, 394057, 394047, 394042, 394004, 393977,
                393952, 393950, 393945, 393944, 393939, 393930, 393923, 393921, 393919, 393917, 393916, 393902, 393890,
                393873, 393871, 393863, 393858, 393855, 393852, 393851, 393815, 393808, 393795, 393765, 393760, 393755,
                393722, 393714, 393711, 393687, 393683, 393671, 393657, 393652, 393651, 393646, 393640, 393629, 393618,
                393613, 393608, 393594, 393574, 393560, 393555, 393551, 393537, 393535, 393525, 393515, 393510, 393500,
                393493, 393488, 393475, 393472, 393464, 393463, 393448, 393426, 393404, 393381, 393365, 393344, 393319,
                393304, 393299, 393297, 393295, 393293, 393291, 393276, 393271, 393214, 393200, 393193, 393183, 393182,
                393176, 393175, 393154, 393153, 393152, 393144, 393143, 393139, 393133, 393128, 393121, 393111, 393103,
                393084, 393079, 393066, 393056, 393050, 393042, 393037, 393032, 393031, 393015, 393014, 393012, 392977,
                392967, 392958, 392951, 392947, 392929, 392920, 392917, 392899, 392895, 392893, 392889, 392884, 392883,
                392869, 392866, 392865, 392857, 392852, 392813, 392805, 392795, 392776, 392767, 392764, 392754, 392746,
                392745, 392737, 392736, 392720, 392718, 392716, 392710, 392697, 392688, 392687, 392685, 392675, 392674,
                392659, 392631, 392587, 392578, 392577, 392559, 392539, 392529, 392514, 392510, 392509, 392508, 392501,
                392499, 392492, 392481, 392473, 392453, 392408, 392404, 392400, 392355, 392348, 392342, 392342, 392327,
                392324, 392321, 392317, 392316, 392308, 392301, 392296, 392295, 392280, 392277, 392273, 392256, 392249,
                392237, 392233, 392232, 392225);
        shopGeoId.put(392222, 2);
        shopGeoId.put(392220, 2);
        SHOPS_WITH_DELIVERY_TO_COUNTRY = ImmutableMap.<Integer, Integer>builder().putAll(shopGeoId).build();
    }

    static {
        //select shop_id from shops where size(regions)=1 and array_contains(regions,213) limit 40
        Map<Integer, Integer> shopGeoId = createMapFromShopIdsWithGeoId(213,
                405339, 405329, 405321, 405320, 405317, 405315, 405314, 405300, 405269, 405264, 405248, 405233, 405224,
                405221, 405189, 405188, 405165, 405137, 405125, 405116, 405032, 405024, 404978, 404960, 404950, 404944,
                404935, 404926, 404911, 404888, 404846, 404832, 404808, 404806, 404798, 404794, 404787, 404772, 404747,
                404745);
        SHOPS_WITH_DELIVERY_TO_REGION = ImmutableMap.<Integer, Integer>builder().putAll(shopGeoId).build();
    }

    static {
        //select shop_id from shops where array_contains(regions,166) limit 3
        Map<Integer, Integer> shopGeoId = createMapFromShopIdsWithGeoId(213, 404057, 399614, 399525);
        SHOPS_FOR_SNG_DELIVERY = ImmutableMap.<Integer, Integer>builder().putAll(shopGeoId).build();
    }

    static {
        //select shop_id from shops where size(regions)=1 and array_contains(regions, 143) limit 80
        Map<Integer, Integer> shopGeoId = createMapFromShopIdsWithGeoId(213,
                405596, 405023, 404575, 404573, 404043, 403994, 403961, 403843, 403838, 403583, 403124, 402564, 402213,
                402189, 401847, 401481, 401187, 400848, 400843, 400816, 400265, 400149, 399982, 399933, 399932, 399576,
                399280, 398956, 398053, 397350, 397277, 397266, 397046, 396826, 396443, 396378, 396134, 396075, 395609,
                395600, 394959, 394382, 394370, 394226, 394138, 392771, 391796, 391781, 391351, 391027, 390741, 390493,
                390347, 389913, 389778, 389414, 389039, 388827, 386686, 386665, 386395, 386114, 386015, 385819, 385114,
                383644, 383297, 381297, 381157, 380722, 380396, 380146, 379914, 378590, 377996, 377772, 377456, 377210,
                376161, 374935);
        shopGeoId.put(84339, 2);
        shopGeoId.put(258600, 8547);
        shopGeoId.put(250197, 27520);
        shopGeoId.put(167350, 67161);
        shopGeoId.put(130505, 106197);
        shopGeoId.put(106180, 106326);
        shopGeoId.put(135518, 108657);
        shopGeoId.put(1876, 111452);
        shopGeoId.put(90617, 111472);
        shopGeoId.put(191705, 113814);
        shopGeoId.put(102499, 115359);
        shopGeoId.put(110002, 106546);
        shopGeoId.put(269407, 113055);
        shopGeoId.put(60723, 24808);
        shopGeoId.put(181194, 21297);
        shopGeoId.put(44154, 102670);
        shopGeoId.put(203826, 115602);
        SHOPS_WITHOUT_DELIVERY_TO_REGION = ImmutableMap.<Integer, Integer>builder().putAll(shopGeoId).build();
    }

    static {
        Map<Integer, Integer> shopGeoId = new HashMap<>();
        //select * from regions where parent_ru_name='Германия' limit 1
        shopGeoId.put(391625, 177);
        //select * from regions where parent_ru_name='Лондон' limit 1
        shopGeoId.put(384101, 123448);
        //select * from regions where parent_ru_name='Австрия' limit 1
        shopGeoId.put(373971, 104128);
        //select * from regions where parent_ru_name='Бельгия' limit 1
        shopGeoId.put(354274, 105770);
        //select * from regions where parent_ru_name='Болгария' limit 1
        shopGeoId.put(254619, 104137);
        //select * from regions where parent_ru_name='Венгрия' limit 1
        shopGeoId.put(212684, 104471);
        SHOPS_FOREIGN_DELIVERY = ImmutableMap.<Integer, Integer>builder().putAll(shopGeoId).build();
    }

    public static int generate() {
        return getRandomShopWithDelivery(213);
    }

    public static Integer generate(int geoId) {
        return getRandomShopWithDelivery(geoId);
    }

    public static Integer testShop() {
        List<Integer> testShopsAllowedForTest = new ArrayList<>(TEST_SHOPS);
        testShopsAllowedForTest.remove(774);
        return RndUtil.choice(testShopsAllowedForTest);
    }

    public static Map<Integer, Integer> getShopsWithDeliveryToCountry() {
        return SHOPS_WITH_DELIVERY_TO_COUNTRY;
    }

    public static Map<Integer, Integer> getShopsWithDeliveryToRegion() {
        return SHOPS_WITH_DELIVERY_TO_REGION;
    }

    public static Map<Integer, Integer> getShopsForSngDelivery() {
        return SHOPS_FOR_SNG_DELIVERY;
    }

    public static Map<Integer, Integer> getShopsWithoutDeliveryToRegion() {
        return SHOPS_WITHOUT_DELIVERY_TO_REGION;
    }

    public static Map<Integer, Integer> getShopsForeignDelivery() {
        return SHOPS_FOREIGN_DELIVERY;
    }

    private static Map<Integer, Integer> createMapFromShopIdsWithGeoId(Integer geoId, Integer... shopIds) {
        return Stream.of(shopIds).map(it -> new Integer[]{it, geoId})
                .collect(Collectors.toMap(it -> it[0], it -> it[1], (key1, key2) -> key2));
    }

    private static int getRandomShopWithDelivery(int geoId) {
        Map<Integer, Integer> shopsWithGeo = RndUtil.nextBool() ? getShopsWithDeliveryToRegion()
                : getShopsWithDeliveryToCountry();
        List<Integer> shopsWithDelivery = shopsWithGeo.entrySet().stream().filter(e -> e.getValue().equals(geoId))
                .map(e -> e.getKey()).collect(toList());
        if (shopsWithDelivery.isEmpty()) {
            throw new IllegalArgumentException("Сan't find shop with delivery for geo_id " + geoId);
        }
        return RndUtil.choice(shopsWithDelivery);
    }
}
