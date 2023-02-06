package ru.yandex.market.loyalty.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdmin;
import ru.yandex.market.loyalty.admin.controller.convert.AccrualPromoDtoConverter;
import ru.yandex.market.loyalty.admin.controller.dto.AccrualPromoDto;
import ru.yandex.market.loyalty.admin.controller.dto.IdObject;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.model.promo.CorePromoType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoSubType;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.io.IOException;
import java.util.Date;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestFor(AccrualPromoController.class)
public class AccrualPromoControllerTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String NEW_PROMO_NAME = "new_promo_name";
    private static final Date DEFAULT_START_DATE = new Date();
    private static final int PROMO_DURATION = 3;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PromoService promoService;
    @MarketLoyaltyAdmin
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AccrualPromoDtoConverter accrualPromoDtoConverter;
    @Autowired
    private PromoUtils promoUtils;

    @Test
    public void shouldCreatePromo() throws Exception {
        AccrualPromoDto accrualPromoDto = createAccrualPromoDto();

        long newPromoId = createAccrualPromo(
                accrualPromoDto
        );
        Promo promo = promoService.getPromo(newPromoId);
        AccrualPromoDto resultPromoDto = accrualPromoDtoConverter.toDto(promo);
        assertThat(
                resultPromoDto,
                samePropertyValuesAs(accrualPromoDto, "id", "status", "promoKey", "emissionMatter", "creator",
                        "lastEditor", "customPromoCodeGeneratorType", "modificationTime"
                )
        );
        assertEquals(PromoStatus.ACTIVE, resultPromoDto.getStatus());
    }

    @Test
    public void shouldUpdateStatusToInactiveAndBackToActive() throws Exception {
        Promo promo = promoUtils.buildWalletAccrualPromo(defaultBuilder(NEW_PROMO_NAME));
        long promoId = promo.getId();

        changeStatus(promoId, PromoStatus.INACTIVE);

        assertEquals(PromoStatus.INACTIVE, promoService.getPromo(promoId).getStatus());

        changeStatus(promoId, PromoStatus.ACTIVE);

        assertEquals(PromoStatus.ACTIVE, promoService.getPromo(promoId).getStatus());
    }

    @Test
    public void shouldGetCreatedPromo() throws Exception {
        Promo promo = promoUtils.buildWalletAccrualPromo(defaultBuilder(NEW_PROMO_NAME));


        String accrualPromoDtoStr = mockMvc
                .perform(get("/api/accrual-promo/" + promo.getId()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse().getContentAsString();

        AccrualPromoDto returnedPromo = objectMapper.readValue(accrualPromoDtoStr, AccrualPromoDto.class);
        AccrualPromoDto storedPromo = accrualPromoDtoConverter.toDto(promoService.getPromo(promo.getId()));
        assertThat(returnedPromo, samePropertyValuesAs(storedPromo));
    }

    @Test
    public void shouldUpdatePromo() throws Exception {
        Promo promo = promoUtils.buildWalletAccrualPromo(defaultBuilder(NEW_PROMO_NAME));

        AccrualPromoDto storedPromo = accrualPromoDtoConverter.toDto(promo);

        String newDescription = "new description";
        storedPromo.setDescription(newDescription);

        updatePromo(storedPromo);

        assertEquals(promoService.getPromo(promo.getId()).getDescription(), newDescription);
    }

    private long createAccrualPromo(AccrualPromoDto accrualPromoDto) throws Exception {
        return extractId(mockMvc
                .perform(multipart("/api/accrual-promo/create")
                        .param("accrualPromo", objectMapper.writeValueAsString(accrualPromoDto))
                        .with(csrf())
                )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse().getContentAsString());
    }

    private long extractId(String idResult) throws IOException {
        return Long.parseLong(objectMapper.readValue(idResult, IdObject.class).getId());
    }

    private PromoUtils.AccrualPromoBuilder defaultBuilder(String promoName) {
        return promoUtils.accrualPromoBuilder()
                .setName(promoName)
                .setStartDate(DEFAULT_START_DATE)
                .setEndDate(DateUtils.addDays(DEFAULT_START_DATE, PROMO_DURATION));
    }

    private static AccrualPromoDto createAccrualPromoDto() {
        AccrualPromoDto accrualPromoDto = new AccrualPromoDto();
        accrualPromoDto.setName(NEW_PROMO_NAME);
        accrualPromoDto.setDescription(PromoUtils.DEFAULT_DESCRIPTION);
        accrualPromoDto.setStartDate(DEFAULT_START_DATE);
        accrualPromoDto.setStatus(PromoStatus.ACTIVE);
        accrualPromoDto.setEndDate(DateUtils.addDays(DEFAULT_START_DATE, PROMO_DURATION));
        accrualPromoDto.setMarketPlatform(MarketPlatform.BLUE);
        accrualPromoDto.setPromoType(CorePromoType.ACCRUAL);
        accrualPromoDto.setPromoSubType(PromoSubType.CASHBACK_ACCRUAL);
        accrualPromoDto.setPromoSource(LOYALTY_VALUE);
        return accrualPromoDto;
    }

    private void changeStatus(long promoId, PromoStatus promoStatus) throws Exception {
        mockMvc
                .perform(
                        put("/api/accrual-promo/{promoId}/changeStatus/{status}", promoId, promoStatus.getCode())
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .with(csrf())
                )
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    private void updatePromo(AccrualPromoDto accrualPromoDto) throws Exception {
        mockMvc
                .perform(put("/api/accrual-promo/update")
                        .content(objectMapper.writeValueAsBytes(accrualPromoDto))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .with(csrf())
                )
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()));
    }

}
