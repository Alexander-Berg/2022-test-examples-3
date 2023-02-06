package ru.yandex.market.billing.distribution.imports.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.distribution.share.model.DistributionPartner;
import ru.yandex.market.billing.distribution.share.model.DistributionPartnerSegment;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

public class HardcodedClidDaoTest extends FunctionalTest {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private HardcodedClidDao dao;

    @BeforeEach
    public void setup() {
        dao = new HardcodedClidDao(namedParameterJdbcTemplate);
    }

    @Test
    @DbUnitDataSet(before = "HardcodedClidDaoTest.before.csv", after = "HardcodedClidDaoTest.insert.after.csv")
    void insert() {
        dao.insert(List.of(
                DistributionPartner.builder()
                        .setClid(6L)
                        .setPartnerSegment(DistributionPartnerSegment.CLOSER)
                        .setPlaceType("Купонный агрегатор")
                        .build()));
    }

    @Test
    @DbUnitDataSet(before = "HardcodedClidDaoTest.before.csv", after = "HardcodedClidDaoTest.update.after.csv")
    void update() {
        dao.update(List.of(
                DistributionPartner.builder()
                        .setClid(2L)
                        .setPartnerSegment(DistributionPartnerSegment.MARKETING)
                        .setPlaceType("Twitch канал")
                        .build(),
                DistributionPartner.builder()
                        .setClid(5L)
                        .setPartnerSegment(DistributionPartnerSegment.CLOSER)
                        .setPlaceType(null)
                        .build()
        ));
    }

    @Test
    @DbUnitDataSet(before = "HardcodedClidDaoTest.before.csv", after = "HardcodedClidDaoTest.delete.after.csv")
    void delete() {
        dao.delete(List.of(2L, 3L));
    }

    @Test
    @DbUnitDataSet(before = "HardcodedClidDaoTest.before.csv")
    void select() {
        Map<Long, DistributionPartner.Builder> input = Map.of(
                1L, DistributionPartner.builder().setClid(1L),
                4L, DistributionPartner.builder().setClid(4L),
                100L, DistributionPartner.builder().setClid(100L)
        );
        dao.getPartnerData(List.of(1L, 4L, 100L), pd ->
                input.get(pd.getClid())
                        .setPartnerSegment(pd.getPartnerSegment())
                        .setPlaceType(pd.getPlaceType())
        );
        assertThat(input.get(1L).build().getPartnerSegment(), is(DistributionPartnerSegment.MARKETING));
        assertThat(input.get(4L).build().getPartnerSegment(), is(DistributionPartnerSegment.CLOSER));
        assertThat(input.get(100L).build().getPartnerSegment(), nullValue());
    }

    @Test
    @DbUnitDataSet(before = "HardcodedClidDaoTest.before.csv")
    void getAll() {
        List<DistributionPartner> result = dao.getAll()
                .stream()
                .map(DistributionPartner.Builder::build)
                .collect(Collectors.toList());
        assertThat(result, containsInAnyOrder(
                DistributionPartner.builder()
                        .setClid(1L)
                        .setPartnerSegment(DistributionPartnerSegment.MARKETING)
                        .setPlaceType("Instagram блог")
                        .build(),
                DistributionPartner.builder()
                        .setClid(2L)
                        .setPartnerSegment(DistributionPartnerSegment.CLOSER)
                        .setPlaceType(null)
                        .build(),
                DistributionPartner.builder()
                        .setClid(3L)
                        .setPartnerSegment(DistributionPartnerSegment.CLOSER)
                        .setPlaceType("Кэшбэк-сервис")
                        .build(),
                DistributionPartner.builder()
                        .setClid(4L)
                        .setPartnerSegment(DistributionPartnerSegment.CLOSER)
                        .setPlaceType("Кэшбэк-сервис")
                        .build(),
                DistributionPartner.builder()
                        .setClid(5L)
                        .setPartnerSegment(DistributionPartnerSegment.MARKETING)
                        .setPlaceType("Twitch канал")
                        .build()
        ));
    }

    @Test
    @DbUnitDataSet(before = "HardcodedClidDaoTest.before.csv")
    void getUpdatedBetween() {
        List<Long> result = dao.getUpdatedBetween(
                LocalDateTime.parse("2022-03-30T00:00:05"),
                        LocalDateTime.parse("2022-03-30T08:00:00"));
        assertThat(result, containsInAnyOrder(2L, 5L));
    }
}