package ru.yandex.market.wms.core.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils

class BalancesControllerTest : IntegrationTest() {

    /**
     * Получение балансов по фильтру, содержащиму идентификатор ячейки
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getBalancesByParamsWithLocHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=loc==STAGE&sort=loc"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoBalancesByParamsWithLoc() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=loc==1-01"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getExtendedBalancesByParamsWithLocHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=loc==STAGE&sort=loc"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoExtendedBalancesByParamsWithLoc() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=loc==1-01"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    /**
     * Получение балансов по фильтру, содержащиму идентификатор SKU
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getBalancesByParamsWithSkuHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=sku==ROV0000000000000056014&sort=sku"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoBalancesByParamsWithSku() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=sku==ROV12345"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getExtendedBalancesByParamsWithSkuHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=sku==ROV0000000000000056014&sort=sku"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoExtendedBalancesByParamsWithSku() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=sku==ROV12345"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    /**
     * Получение балансов по фильтру, содержащиму Наименование
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getBalancesByParamsWithNameHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=name=='test item 1'&sort=name"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoBalancesByParamsWithName() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=name=='name'"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getExtendedBalancesByParamsWithNameHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=name=='test item 1'&sort=name"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoExtendedBalancesByParamsWithName() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=name=='name'"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    /**
     * Получение балансов по фильтру, содержащиму Артикул поставщика
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getBalancesByParamsWithManufacturerSkuHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=manufacturerSku=='100246556660'&sort=manufacturerSku"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoBalancesByParamsWithManufacturerSku() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=manufacturerSku=='111'"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getExtendedBalancesByParamsWithManufacturerSkuHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=manufacturerSku=='100246556660'&sort=manufacturerSku"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoExtendedBalancesByParamsWithManufacturerSku() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=manufacturerSku=='222'"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    /**
     * Получение балансов по фильтру, содержащиму идентификатор НЗН
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getBalancesByParamsWithIdHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=id==ID_TEST&sort=id"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup-virtual.xml")
    @Throws(Exception::class)
    fun getVirtualBalancesByParamsWithIdHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=id==ID_TEST&sort=id"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-virtual.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoBalancesByParamsWithId() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=id==ID"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getExtendedBalancesByParamsWithIdHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=id==ID_TEST&sort=id"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoExtendedBalancesByParamsWithId() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=id==ID"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    /**
     * Получение балансов по фильтру, содержащиму Идентификатор производителя
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getBalancesByParamsWithStorerKeyHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=storerKey==465852&sort=storerKey"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoBalancesByParamsWithStorerKey() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=storerKey==123"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getExtendedBalancesByParamsWithStorerKeyHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=storerKey==465852&sort=storerKey"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoExtendedBalancesByParamsWithStorerKey() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=storerKey==123"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    /**
     * Получение балансов по фильтру, содержащиму серийный номер
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getExtendedBalancesByParamsWithSerialNumberHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=serialNumber==4501493110,serialNumber==5604150924&sort=serialNumber"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoExtendedBalancesByParamsWithSerialNumber() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=serialNumber==101"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getExtendedBalancesByParamsWithSerialNumberLongHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=serialNumberLong==4501493110,serialNumberLong==5604150924&sort=serialNumberLong"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoExtendedBalancesByParamsWithSerialNumberLong() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=serialNumberLong==101"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    /**
     * Получение балансов по фильтру, содержащиму Количество
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getBalancesByParamsWithQtyHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=qty>=3&sort=qty"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup-some.xml")
    @Throws(Exception::class)
    fun getSomeBalancesByParamsWithQtyHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=qty>=0&sort=qty"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-some.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoBalancesByParamsWithQty() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=qty==10"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getBalancesByParamsWithQtyAllocatedHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=qtyAllocated>=1&sort=qtyAllocated"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoBalancesByParamsWithQtyAllocated() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=qtyAllocated==10"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getBalancesByParamsWithQtyPickedHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=qtyPicked>=2&sort=qtyPicked"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoBalancesByParamsWithQtyPicked() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=qtyPicked==10"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getBalancesByParamsWithQtyExpectedHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=qtyExpected>=1&sort=qtyExpected"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoBalancesByParamsWithQtyExpected() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=qtyExpected==10"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getBalancesByParamsWithQtyInProgressHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=qtyInProgress==0&sort=qtyInProgress"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoBalancesByParamsWithQtyInProgress() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=qtyInProgress==10"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getExtendedBalancesByParamsWithQtyHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=qty==2&sort=qty"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended-single.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoExtendedBalancesByParamsWithQty() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=qty==10"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    /**
     * Получение балансов по фильтру, содержащиму Вес
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getExtendedBalancesByParamsWithGrossWeightHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=grossWeight==5&sort=grossWeight"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended-single.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoExtendedBalancesByParamsWithGrossWeight() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=grossWeight>=10"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getExtendedBalancesByParamsWithNetWeightHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=netWeight<6&sort=netWeight"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getExtendedBalancesByParamsWithTareWeightHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=tareWeight<=2&sort=tareWeight"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended.json")))
            .andReturn()
    }

    /**
     * Получение балансов по фильтру, содержащиму Даты создания и Дата изменения
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getBalancesByParamsWithAddDateHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=addDate<=2020-01-01T00:00:00&sort=addDate"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoBalancesByParamsWithAddDate() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=addDate>2020-01-01T00:00:00"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getExtendedBalancesByParamsWithAddDateHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=addDate<=2020-03-03T00:00:00&sort=addDate"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getSingleExtendedBalanceByParamsWithAddDateHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=addDate==2020-01-01T00:00:00&sort=addDate"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended-single.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoExtendedBalancesByParamsWithAddDate() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=addDate>2020-03-03T00:00:00"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getBalancesByParamsWithEditDateHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=editDate==2020-02-02T00:00:00&sort=editDate"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoBalancesByParamsWithEditDate() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=editDate>2020-04-04T00:00:00"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getSingleExtendedBalanceByParamsWithEditDateHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=editDate==2020-02-02T00:00:00&sort=editDate"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended-single.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoExtendedBalancesByParamsWithEditDate() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=editDate>2020-04-04T00:00:00"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    /**
     * Получение балансов по фильтру, содержащиму поле Кто создал
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getBalancesByParamsWithAddWhoHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=addWho==TEST1&sort=addWho"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoBalancesByParamsWithAddWho() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=addWho==NO"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getExtendedBalanceыByParamsWithAddWhoHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=addWho==TEST1&sort=addWho"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoExtendedBalancesByParamsWithAddWho() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=addWho==NO"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    /**
     * Получение балансов по фильтру, содержащиму поле Кто изменил
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getBalancesByParamsWithEditWhoHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=editWho==TEST2&sort=editWho"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoBalancesByParamsWithEditWho() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=editWho==NO"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getExtendedBalanceыByParamsWithEditWhoHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=editWho==TEST2&sort=editWho"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoExtendedBalancesByParamsWithEditWho() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=editWho==NO"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    /**
     * Получение балансов по фильтру, содержащиму Статус
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getBalancesByParamsWithStatusHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=status=='OK'&sort=status"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoBalancesByParamsWithStatus() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances?filter=status=='LOST'"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    /**
     * Получение балансов по фильтру, содержащиму Идентификатор виртуальных серийников
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getExtendedBalancesByParamsWithFakeHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=fake==0&sort=fake"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/response-extended.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNoExtendedBalancesByParamsWithFake() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended?filter=fake==1"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    /**
     * Получение балансов по искомым параметрам не содержащим фильтров
     */
    @Test
    @Throws(Exception::class)
    fun getNoBalances() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun getNoExtendedBalances() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/extended"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/2/empty-response.json")))
            .andReturn()
    }

    /**
     * Получение балансов по идентификатору ячейки
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getItemsByLocHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-loc/STAGE"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/1/response-by-loc.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNothingByLoc() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-loc/1-01"))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun getItemsByNullLoc() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-loc/"))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }

    /**
     * Получение балансов по идентификатору SKU
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getItemsBySkuHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-sku/ROV0000000000000056014"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/1/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNothingBySku() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-sku/ROV12345"))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun getItemsByNullSku() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-sku/"))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }

    /**
     * Получение балансов по идентификатору НЗН
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getItemsByIdHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-id/ID_TEST"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/1/response.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup-virtual.xml")
    @Throws(Exception::class)
    fun getVirtualItemsByIdHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-id/ID_TEST"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/1/response-virtual.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getItemsByIdWithParentIdHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-id/ID_TEST"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/1/response-with-parentid.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup-with-childids.xml")
    @Throws(Exception::class)
    fun getItemsByIdWithChildIdHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-id/ID_TEST"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/1/response-with-childids.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNothingItemsById() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-id/ID"))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }

    /**
     * Получение балансов по серийному номеру
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getItemsBySerialNumberHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-sn/4501493110"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/1/responseBySN.json")))
            .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun getItemsByNullSerialNumber() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-sn/"))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNothingBySerialNumber() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-sn/SERIALNUMBER"))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }

    /**
     * Получение балансов по уникальному идентификатору
     */
    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getItemsByIdentityWhenIdentityIsUit() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-identity/5604150924"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/1/response-by-identity.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getItemsByIdentityWhenIdentityIsImei() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-identity/490154203237518"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/1/response-by-identity.json")))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/controller/balances/setup.xml")
    @Throws(Exception::class)
    fun getNothingByIdentity() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-identity/IDENTITY"))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }

    /**
     * Получение балансов по паллете
     */
    @Test
    @DatabaseSetup("/controller/balances/setup-pallet.xml")
    @Throws(Exception::class)
    fun getItemsByPalletHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/balances/by-pallet/PLT0000001"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent("controller/balances/1/response-pallet.json")))
            .andReturn()
    }
}
