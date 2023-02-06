package ru.yandex.market.wms.receiving.service;

import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.wms.common.spring.dao.entity.AltSku;
import ru.yandex.market.wms.common.spring.dao.implementation.AltSkuDao;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.service.returns.AltSkuService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AltSkuServiceTest extends ReceivingIntegrationTest {

    private static final String USER = "TEST";

    private static final String NEW_ALTSKU = "NEWALTSKU";
    private static final String NEW_SKU = "NEWSKU";
    private static final String NEW_STORERKEY = "NEWSTORER";

    private static final String EXISTS_ALTSKU = "ALTSKU1234567";
    private static final String EXISTS_SKU = "ROV123";
    private static final String EXISTS_STORERKEY = "STORER1";

    @Autowired
    private AltSkuService altSkuService;

    @Autowired
    @SpyBean
    private AltSkuDao altSkuDao;

    @AfterEach
    public void reset() {
        Mockito.reset(altSkuDao);
    }

    @Test
    @DatabaseSetup("/service/alt-sku/create-alt-sku-before.xml")
    @ExpectedDatabase(value = "/service/alt-sku/create-new-alt-sku-after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void createNotExistsAltSku() {
        AltSku expected = AltSku.builder()
            .sku(NEW_SKU)
            .storerKey(NEW_STORERKEY)
            .altsku(NEW_ALTSKU)
            .build();
        AltSku result = altSkuService.findOrCreateAltSku(expected);

        assertEquals(expected, result);
        verifyFind(expected);
        verify(altSkuDao).insertAltSkuWithUser(eq(expected), eq(USER));
        verifyNoMoreInteractions(altSkuDao);
    }

    @Test
    @DatabaseSetup("/service/alt-sku/create-alt-sku-before.xml")
    @ExpectedDatabase(value = "/service/alt-sku/create-alt-sku-before.xml", assertionMode = NON_STRICT_UNORDERED)
    public void tryToCreateAlreadyExistsAltSku() {
        AltSku expected = AltSku.builder()
            .sku(EXISTS_SKU)
            .storerKey(EXISTS_STORERKEY)
            .altsku(EXISTS_ALTSKU)
            .build();

        AltSku result = altSkuService.findOrCreateAltSku(expected);

        assertEquals(expected, result);
        verifyFind(expected);
        verifyNoMoreInteractions(altSkuDao);
    }

    @Test
    @DatabaseSetup("/service/alt-sku/create-alt-sku-before.xml")
    @ExpectedDatabase(value = "/service/alt-sku/create-new-alt-sku-with-exists-sku-and-storerkey-after.xml",
        assertionMode = NON_STRICT_UNORDERED)
    public void tryToCreateNewAltSkuWithExistsSkuAndStorerKey() {
        AltSku expected = AltSku.builder()
            .sku(EXISTS_SKU)
            .storerKey(EXISTS_STORERKEY)
            .altsku(NEW_ALTSKU)
            .build();

        AltSku result = altSkuService.findOrCreateAltSku(expected);

        assertEquals(expected, result);
        verifyFind(expected);
        verify(altSkuDao).insertAltSkuWithUser(eq(expected), eq(USER));
        verifyNoMoreInteractions(altSkuDao);
    }

    @Test
    @DatabaseSetup("/service/alt-sku/create-alt-sku-before.xml")
    @ExpectedDatabase(value = "/service/alt-sku/create-new-alt-sku-with-exists-alt-sku-after.xml",
        assertionMode = NON_STRICT_UNORDERED)
    public void tryToCreateNewAltSkuWithExistsAltSku() {
        AltSku expected = AltSku.builder()
            .sku(NEW_SKU)
            .storerKey(NEW_STORERKEY)
            .altsku(EXISTS_ALTSKU)
            .build();

        AltSku result = altSkuService.findOrCreateAltSku(expected);

        assertEquals(expected, result);
        verifyFind(expected);
        verify(altSkuDao).insertAltSkuWithUser(eq(expected), eq(USER));
        verifyNoMoreInteractions(altSkuDao);
    }


    private void verifyFind(AltSku expected) {
        verify(altSkuDao).findAltSku(eq(expected));
        verify(altSkuDao).findExistingAltSkus(eq(Collections.singleton(expected)));
    }
}
