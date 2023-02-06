package ru.yandex.direct.core.entity.vcard.repository.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.vcard.model.PointOnMap;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.vcard.service.validation.PointOnMapValidator.LATITUDE_MAX;
import static ru.yandex.direct.core.entity.vcard.service.validation.PointOnMapValidator.LATITUDE_MIN;
import static ru.yandex.direct.core.entity.vcard.service.validation.PointOnMapValidator.LONGITUDE_MAX;
import static ru.yandex.direct.core.entity.vcard.service.validation.PointOnMapValidator.LONGITUDE_MIN;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MapsRepositoryTest {

    @Autowired
    private MapsRepository mapsRepository;

    private int shard = 1;

    // getOrCreatePointOnMap - проверка сохраненных данных - новые элементы

    @Test
    public void getOrCreatePointOnMap_OneNewItem_ReturnsOneId() {
        PointOnMap point = randomPoint();
        List<Long> ids = mapsRepository.getOrCreatePointOnMap(shard, singletonList(point));
        assertThat("метод должен вернуть список, состоящий из одного id", ids, contains(greaterThan(0L)));
    }

    @Test
    public void getOrCreatePointOnMap_OneNewItem_CreatesValidPoint() {
        PointOnMap point = randomPoint();
        List<Long> ids = create(point);
        Map<Long, PointOnMap> points = get(ids);
        assertThat(points.get(ids.get(0)), beanDiffer(point));
    }

    @Test
    public void getOrCreatePointOnMap_TwoDifferentNewItems_CreatesValidPoints() {
        PointOnMap point1 = randomPoint();
        PointOnMap point2 = randomPoint();
        List<Long> ids = create(point1, point2);
        Map<Long, PointOnMap> points = get(ids);
        assertThat(points.get(ids.get(0)), beanDiffer(point1));
        assertThat(points.get(ids.get(1)), beanDiffer(point2));
    }

    @Test
    public void getOrCreatePointOnMap_TwoEqualNewItems_CreatesValidPoint() {
        PointOnMap point1 = randomPoint();
        PointOnMap point2 = copyPoint(point1);
        List<Long> ids = create(point1, point2);

        checkState(ids.get(0).equals(ids.get(1)),
                "для обоих создаваемых элементов возвращаемые id должны быть одинаковы");

        Map<Long, PointOnMap> points = mapsRepository.getPoints(shard, ids);
        assertThat(points.get(ids.get(0)), beanDiffer(point1));
    }

    @Test
    public void getOrCreatePointOnMap_OneNewAndOneMatchingItem_CreatesValidPointAndReturnIdOfExistingPoint() {
        PointOnMap point1 = randomPoint();
        List<Long> idsOld = create(point1);

        PointOnMap point11 = copyPoint(point1);
        PointOnMap point2 = randomPoint();
        List<Long> ids = create(point11, point2);

        checkState(ids.get(0).equals(idsOld.get(0)),
                "возвращенный id для элемента, соответствующего существующему в базе, "
                        + "должен быть равен существующему id");

        Map<Long, PointOnMap> points = get(ids);
        assertThat(points.get(ids.get(0)), beanDiffer(point1));
        assertThat(points.get(ids.get(1)), beanDiffer(point2));
    }

    // getOrCreatePointOnMap - проверка уникализации - существующие элементы

    @Test
    public void getOrCreatePointOnMap_OneMatchingItem_ReturnsExistingId() {
        PointOnMap point1 = randomPoint();
        PointOnMap point11 = copyPoint(point1);
        List<Long> oldIds = create(point1);
        List<Long> ids = create(point11);
        assertThat("возвращаемый id элемента, соответствующего существующему в базе, "
                        + "должен быть равен существующему id",
                ids.get(0), equalTo(oldIds.get(0)));
    }

    @Test
    public void getOrCreatePointOnMap_OneMatchingItemWithOtherScale_ReturnsExistingId() {
        PointOnMap point1 = randomPoint();
        List<Long> oldIds = create(point1);

        PointOnMap point11 = copyPoint(point1);
        BigDecimal newX = point11.getX().setScale(point11.getX().scale() + 1, BigDecimal.ROUND_CEILING);
        point11.setX(newX);
        List<Long> ids = create(point11);

        assertThat("возвращаемый id элемента, соответствующего существующему в базе, "
                        + "должен быть равен существующему id",
                ids.get(0), equalTo(oldIds.get(0)));
    }

    @Test
    public void getOrCreatePointOnMap_TwoEqualItemsMatchingItemsInDatabase_ReturnsExistingId() {
        PointOnMap point1 = randomPoint();
        PointOnMap point11 = copyPoint(point1);
        PointOnMap point12 = copyPoint(point1);

        List<Long> oldIds = create(point1);
        List<Long> ids = create(point11, point12);

        assertThat("возвращаемые id для одинаковых элементов должны быть равны",
                ids.get(0), equalTo(ids.get(1)));
        assertThat("возвращаемый id элемента, соответствующего существующему в базе, "
                        + "должен быть равен существующему id",
                ids.get(0), equalTo(oldIds.get(0)));
    }

    @Test
    public void getOrCreatePointOnMap_TwoDifferentItemsMatchingItemsInDatabase_ReturnsExistingId() {
        PointOnMap point1 = randomPoint();
        PointOnMap point2 = randomPoint();
        PointOnMap point11 = copyPoint(point1);
        PointOnMap point21 = copyPoint(point2);

        List<Long> oldIds = create(point1, point2);
        List<Long> ids = create(point11, point21);

        checkState(!ids.get(0).equals(ids.get(1)),
                "возвращаемые id разных элементов не должны быть равны между собой");

        assertThat("возвращаемый id элемента, соответствующего существующему в базе, "
                        + "должен быть равен существующему id",
                ids.get(0), equalTo(oldIds.get(0)));
        assertThat("возвращаемый id элемента, соответствующего существующему в базе, "
                        + "должен быть равен существующему id",
                ids.get(1), equalTo(oldIds.get(1)));
    }

    // getOrCreatePointOnMap - проверка уникализации - отличающиеся элементы

    @Test
    public void getOrCreatePointOnMap_OneNewItem_ReturnsNewId() {
        PointOnMap point1 = randomPoint().withX(BigDecimal.valueOf(1L));
        PointOnMap point2 = copyPoint(point1).withX(BigDecimal.valueOf(0L));

        List<Long> oldIds = create(point1);
        List<Long> ids = create(point2);

        assertThat("возвращаемый id элемента, не соответствующего существующему в базе, "
                        + "не должен быть равен существующему id",
                ids.get(0), not(equalTo(oldIds.get(0))));
    }

    private List<Long> create(PointOnMap... pointsOnMap) {
        List<Long> ids = mapsRepository.getOrCreatePointOnMap(shard, asList(pointsOnMap));
        checkState(ids != null && ids.size() == pointsOnMap.length,
                "количество возвращаемых id должно быть равно количеству элементов, переданных в метод");
        return ids;
    }

    private Map<Long, PointOnMap> get(List<Long> ids) {
        Map<Long, PointOnMap> points = mapsRepository.getPoints(shard, ids);

        checkState(points.keySet().containsAll(ids),
                "в возвращаемой мапе должны присутствовать ключи - id запрашиваемых элементов");
        checkState(points.size() == ids.size(),
                "размер возвращаемой мапы должен соответствовать количеству запрошенных id");
        return points;
    }

    private static PointOnMap randomPoint() {
        return new PointOnMap()
                .withX(randomX())
                .withY(randomY())
                .withX1(randomX())
                .withY1(randomY())
                .withX2(randomX())
                .withY2(randomY());
    }

    private static BigDecimal randomX() {
        checkState(LONGITUDE_MIN.intValue() < 0 && LONGITUDE_MAX.intValue() > 0);
        int randomInt = nextInt(0, -LONGITUDE_MIN.intValue() + LONGITUDE_MAX.intValue()) +
                LONGITUDE_MIN.intValue();
        return BigDecimal.valueOf(randomInt).setScale(6, RoundingMode.CEILING);
    }


    private static BigDecimal randomY() {
        checkState(LATITUDE_MIN.intValue() < 0 && LATITUDE_MAX.intValue() > 0);
        int randomInt = nextInt(0, -LATITUDE_MIN.intValue() + LATITUDE_MAX.intValue()) +
                LATITUDE_MIN.intValue();
        return BigDecimal.valueOf(randomInt).setScale(6, RoundingMode.CEILING);
    }

    private static PointOnMap copyPoint(PointOnMap point) {
        return new PointOnMap()
                .withX(point.getX())
                .withY(point.getY())
                .withX1(point.getX1())
                .withY1(point.getY1())
                .withX2(point.getX2())
                .withY2(point.getY2());
    }
}
