package ru.yandex.market.mboc.common.datacamp.repository;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

public class TempImportChangeDeltaRepositoryTest extends BaseDbTestClass {
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private TempImportChangeDeltaRepository tempImportChangeDeltaRepository;

    @Test
    public void testOffers() {
        tempImportChangeDeltaRepository.insertOffer(
                OfferTestUtils.nextOffer(),
                OfferTestUtils.nextOffer(),
                OfferTestUtils.nextOffer(), true);
        jdbcTemplate.query("select * from " + TempImportChangeDeltaRepository.TABLE_NAME,
                EmptySqlParameterSource.INSTANCE, rs -> {
                    Assertions.assertThat(rs.getInt("id")).isNotZero();
                    Assertions.assertThat(rs.getString("type")).isEqualTo("offer");
                    Assertions.assertThat(rs.getString("before")).isNotBlank();
                    Assertions.assertThat(rs.getString("after")).isNotBlank();
                    Assertions.assertThat(rs.getString("current")).isNotBlank();
                    Assertions.assertThat(rs.getBoolean("changed")).isTrue();
                });
    }

    @Test
    public void testContent() {
        tempImportChangeDeltaRepository.insertContent(
                OfferTestUtils.nextOffer().extractOfferContent(),
                OfferTestUtils.nextOffer().extractOfferContent(),
                OfferTestUtils.nextOffer().extractOfferContent(),
                false
        );
        jdbcTemplate.query("select * from " + TempImportChangeDeltaRepository.TABLE_NAME,
                EmptySqlParameterSource.INSTANCE, rs -> {
                    Assertions.assertThat(rs.getInt("id")).isNotZero();
                    Assertions.assertThat(rs.getString("type")).isEqualTo("content");
                    Assertions.assertThat(rs.getString("before")).isNotBlank();
                    Assertions.assertThat(rs.getString("after")).isNotBlank();
                    Assertions.assertThat(rs.getString("current")).isNotBlank();
                    Assertions.assertThat(rs.getBoolean("changed")).isFalse();
                });
    }
}
