package ru.yandex.market.logistics.tarifficator.controller;

import javax.annotation.Nonnull;

import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.base.AbstractActivateWithdrawPriceListFileTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class WithdrawPriceListFileControllerTest extends AbstractActivateWithdrawPriceListFileTest {
    @Nonnull
    @Override
    protected ResultActions activateFile(long priceListFileId) throws Exception {
        return mockMvc.perform(post("/withdraw-price-list/files/" + priceListFileId + "/activate"));
    }
}
