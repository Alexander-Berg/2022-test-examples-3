package ru.yandex.market.logistics.management.repository;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.PartnerSubtype;
import ru.yandex.market.logistics.management.domain.entity.PartnerSubtypeFeatures;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.exception.EntityNotFoundException;

@DatabaseSetup("/data/repository/partner_subtype/partner_subtype.xml")
class PartnerSubtypeRepositoryTest extends AbstractContextualTest {
    public static final PartnerSubtype SUB_TYPE_1 = new PartnerSubtype()
        .setId(1001L)
        .setPartnerType(PartnerType.DROPSHIP)
        .setName("Подтип 1")
        .setFeatures(new PartnerSubtypeFeatures().setDefaultMarketBrandedLogisticsPoints(false));
    public static final PartnerSubtype SUB_TYPE_2 = new PartnerSubtype()
        .setId(1002L)
        .setPartnerType(PartnerType.DROPSHIP)
        .setName("Подтип 2")
        .setFeatures(new PartnerSubtypeFeatures().setDefaultMarketBrandedLogisticsPoints(true));
    @Autowired
    private PartnerSubtypeRepository repository;

    @Test
    void findByIdOrThrow() {
        softly
            .assertThat(repository.findByIdOrThrow(1002L))
            .isEqualTo(SUB_TYPE_2);
    }

    @Test
    void findByIdOrThrowFail() {
        softly
            .assertThatThrownBy(() -> repository.findByIdOrThrow(1004L))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findAllByPartnerType() {
        softly
            .assertThat(repository.findAllByPartnerType(PartnerType.DROPSHIP, Pageable.unpaged()))
            .isEqualTo(new PageImpl<>(List.of(SUB_TYPE_1, SUB_TYPE_2)));
    }

    @Test
    void findAllByPartnerTypeNothing() {
        softly
            .assertThat(repository.findAllByPartnerType(PartnerType.FULFILLMENT, Pageable.unpaged()))
            .isEqualTo(Page.empty());
    }

    @Test
    void findByIdAndPartnerType() {
        softly
            .assertThat(repository.findByIdAndPartnerType(1002L, PartnerType.DROPSHIP))
            .contains(SUB_TYPE_2);
    }

    @Test
    void findByIdAndPartnerTypeNull() {
        softly
            .assertThat(repository.findByIdAndPartnerType(1002L, PartnerType.FULFILLMENT))
            .isEmpty();
    }

    @Test
    void findByIdAndPartnerTypeNotMatching() {
        softly
            .assertThat(repository.findByIdAndPartnerType(1003L, PartnerType.DROPSHIP))
            .isEmpty();
    }
}
