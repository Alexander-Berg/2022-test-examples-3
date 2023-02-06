package ru.yandex.market.mbi.api.controller.supplier;

import java.util.Arrays;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.core.tax.model.TaxSystem;
import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.core.tax.model.VatSource;
import ru.yandex.market.mbi.api.client.entity.supplier.SupplierBaseDTO;
import ru.yandex.market.mbi.api.client.entity.supplier.SupplierExtendedDTO;
import ru.yandex.market.mbi.api.client.entity.supplier.SuppliersExtendedInfoDTO;
import ru.yandex.market.mbi.api.client.entity.vat.VatInfo;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * Функциональные тесты на {@link SupplierInfoController}.
 */
@DbUnitDataSet(before = "SupplierControllerTest.before.csv")
class SupplierControllerTest extends FunctionalTest {

    @Test
    @DisplayName("поставщик находится по идентификатору")
    void getSupplierInfoTest() {
        SupplierBaseDTO expectedSupplier = new SupplierBaseDTO(100L, "поставщик", "supplier.by");
        SupplierBaseDTO actualSupplier = mbiApiClient.getSupplierInfo(100L);
        Assertions.assertEquals(expectedSupplier, actualSupplier);
    }

    @Test
    @DisplayName("расширенная информация о поставщиках по id договора")
    @DbUnitDataSet(before = "getSuppliersExtendedInfo.before.csv")
    void getSuppliersExtendedInfoTest() {
        SuppliersExtendedInfoDTO suppliersExtendedInfo = mbiApiClient.getSuppliersExtendedInfo(Arrays.asList(337744L, 998877L));
        SupplierExtendedDTO expected1 = new SupplierExtendedDTO(110, "поставщик", 1777L, 337744L,
                "787801001", "7775556611", SupplierType.FIRST_PARTY);
        SupplierExtendedDTO expected2 = new SupplierExtendedDTO(200, "поставщик2", 1777L, 337744L,
                "787801001", "7775556611", SupplierType.THIRD_PARTY);
        SupplierExtendedDTO expected3 = new SupplierExtendedDTO(444, "новый_поставщик", 9898L, 998877L,
                "132413241", "4535453545", SupplierType.REAL_SUPPLIER);
        MatcherAssert.assertThat(suppliersExtendedInfo.getSuppliers(), containsInAnyOrder(expected1, expected2, expected3));
    }

    @Test
    @DisplayName("расширенная информация о поставщиках по id договора (в xml)")
    @DbUnitDataSet(before = "getSuppliersExtendedInfo.before.csv")
    void getSuppliersExtendedInfoInXmlTest() {
        ResponseEntity<String> response = FunctionalTestHelper.get(URL_PREFIX + port +
                "/suppliers/by_contract_id?contract-id=337744&contract-id=998877");
        String expectedXml =
                "<suppliers-extended-info>" +
                        "<suppliers>" +
                        "<supplier id=\"110\" name=\"поставщик\" prepay-request-id=\"1777\" contract-id=\"337744\" " +
                        "kpp=\"787801001\" inn=\"7775556611\" supplier-type=\"1P\"/>" +
                        "<supplier id=\"200\" name=\"поставщик2\" prepay-request-id=\"1777\" contract-id=\"337744\" " +
                        "kpp=\"787801001\" inn=\"7775556611\" supplier-type=\"3P\"/>" +
                        "<supplier id=\"444\" name=\"новый_поставщик\" prepay-request-id=\"9898\" contract-id=\"998877\" " +
                        "kpp=\"132413241\" inn=\"4535453545\" supplier-type=\"RS\"/>" +
                        "</suppliers>" +
                        "</suppliers-extended-info>";
        MbiAsserts.assertXmlEquals(expectedXml, response.getBody());
    }

    @Test
    @DisplayName("возвращается ошибка при попытке найти несуществующего поставщика")
    void getSupplierInfoNotFoundTest() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.getSupplierInfo(101L)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );
    }

    @Test
    @DisplayName("возвращается ошибка при попытке обновить несуществующего поставщика")
    void updateNonExistentSupplierInfo() {
        SupplierBaseDTO supplierBaseDTO = new SupplierBaseDTO(101L, "поставщик", "supplier.mr");
        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.setSupplierInfo(101L, supplierBaseDTO, 100L)
        );
        MbiAsserts.assertXmlEquals(
                //language=xml
                "<error><message>Supplier not found: 101</message></error>",
                clientException.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("supplierId в теле запроса не совпадает с supplierId в PathParam при обновлении информации о поставщике")
    void pathValueNotCorrespondsBodyValueWhileUpdatingSupplierInfo() {
        SupplierBaseDTO supplierBaseDTO = new SupplierBaseDTO(101L, "поставщик", "supplier.mr");
        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.setSupplierInfo(100L, supplierBaseDTO, 100L)
        );
        MbiAsserts.assertXmlEquals(
                //language=xml
                "<error><message>supplierId in path doesn't math supplierId in request body</message></error>",
                clientException.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("обновление информации о поставщике (обновление имени)")
    @DbUnitDataSet(after = "updateSupplierName.csv")
    void updateSupplierName() {
        SupplierBaseDTO supplierBaseDTO = new SupplierBaseDTO(100L, "беру", null);
        SupplierBaseDTO response = mbiApiClient.setSupplierInfo(100L, supplierBaseDTO, 100L);
        Assertions.assertEquals(supplierBaseDTO, response);
    }

    @Test
    @DisplayName("обновление информации о поставщике (обновление домена)")
    @DbUnitDataSet(after = "updateSupplierDomain.csv")
    void updateSupplierDomain() {
        SupplierBaseDTO supplierBaseDTO = new SupplierBaseDTO(100L, null, "beru.ru");
        SupplierBaseDTO response = mbiApiClient.setSupplierInfo(100L, supplierBaseDTO, 100L);
        Assertions.assertEquals(supplierBaseDTO, response);
    }

    @Test
    @DisplayName("обновление информации о поставщике (обновление имени и домена)")
    @DbUnitDataSet(after = "updateSupplierNameAndDomain.csv")
    void updateSupplierNameAndDomain() {
        SupplierBaseDTO supplierBaseDTO = new SupplierBaseDTO(100L, "беру", "beru.ru");
        SupplierBaseDTO response = mbiApiClient.setSupplierInfo(100L, supplierBaseDTO, 100L);
        Assertions.assertEquals(supplierBaseDTO, response);
    }


    @Test
    @DisplayName("возвращается ошибка при попытке найти несуществующего поставщика")
    void getSupplierNotFoundTest() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.getSupplierInfo(101L)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );
    }
}
