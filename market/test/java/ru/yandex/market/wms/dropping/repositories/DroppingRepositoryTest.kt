package ru.yandex.market.wms.dropping.repositories

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.qameta.allure.kotlin.junit4.AllureRunner
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.market.exceptions.mappers.ExceptionMapper
import ru.yandex.market.generators.SeedAutoIncrementer
import ru.yandex.market.generators.generateInt
import ru.yandex.market.wms.dropping.api.ConfirmFixedApi
import ru.yandex.market.wms.dropping.api.DroppingApi
import ru.yandex.market.wms.dropping.api.ValidationApi
import ru.yandex.market.wms.dropping.common.CoroutineTestRule
import ru.yandex.market.wms.dropping.data.parsers.DropInfoParser
import ru.yandex.market.wms.dropping.data.parsers.DroppingParcelParser
import ru.yandex.market.wms.dropping.data.parsers.ParcelErrorParser
import ru.yandex.market.wms.dropping.generators.generateDropInfoDTO
import ru.yandex.market.wms.dropping.generators.generateDroppingParcelDTO
import ru.yandex.market.wms.dropping.generators.generateParcelErrorDTO
import ru.yandex.market.wms.dropping.generators.generateParcelsList

@ExperimentalCoroutinesApi
@RunWith(AllureRunner::class)
class DroppingRepositoryTest {

    private lateinit var droppingApi: DroppingApi
    private lateinit var confirmFixedApi: ConfirmFixedApi
    private lateinit var validationApi: ValidationApi
    private lateinit var exceptionMapper: ExceptionMapper

    private lateinit var randomToken: String

    private val droppingParcelParser = DroppingParcelParser()
    private val dropInfoParser = DropInfoParser()
    private val parcelErrorParser = ParcelErrorParser()

    private lateinit var droppingRepository: DroppingRepositoryImpl

    private val seedGenerator = SeedAutoIncrementer()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Before
    fun `SetUp DroppingRepository`() = runTest {
        droppingApi = mockk(relaxed = true)
        confirmFixedApi = mockk(relaxed = true)
        validationApi = mockk(relaxed = true)
        exceptionMapper = mockk()

        randomToken = "token: " + generateInt(seedGenerator.nextState())

        droppingRepository = DroppingRepositoryImpl(
            droppingApi = droppingApi,
            confirmFixedApi = confirmFixedApi,
            validationApi = validationApi,
            exceptionMapper = exceptionMapper,
            droppingParcelParser = droppingParcelParser,
            dropInfoParser = dropInfoParser,
            parcelErrorParser = parcelErrorParser,
            dispatcherProvider = coroutineTestRule.testDispatcherProvider
        )
    }

    @Test
    fun `Test getting random parcel info`() = runTest {
        val randomParcelDTO = generateDroppingParcelDTO(seedGenerator.nextState())
        val body = JsonObject().apply {
            addProperty("parcelId", randomParcelDTO.parcelId)
        }

        coEvery { droppingApi.getParcelInfo(randomToken, body) } returns randomParcelDTO

        val expectedResult = droppingParcelParser.parseToEntity(randomParcelDTO)
        assertEquals(expectedResult, droppingRepository.getParcel(randomToken, randomParcelDTO.parcelId.orEmpty()))

        coVerify { droppingApi.getParcelInfo(randomToken, body) }
    }

    @Test
    fun `Test getting random drop info`() = runTest {
        val randomDropInfoDTO = generateDropInfoDTO(seedGenerator.nextState())
        val body = JsonObject().apply {
            addProperty("dropId", randomDropInfoDTO.dropId)
        }

        coEvery { droppingApi.getDropInfo(randomToken, body) } returns randomDropInfoDTO

        val expectedResult = dropInfoParser.parseToEntity(randomDropInfoDTO)
        assertEquals(expectedResult, droppingRepository.getDropInfo(randomToken, randomDropInfoDTO.dropId.orEmpty()))

        coVerify { droppingApi.getDropInfo(randomToken, body) }
    }

    @Test
    fun `Test putting random parcels on drop`() = runTest {
        val randomDropInfoDTO = generateDropInfoDTO(seedGenerator.nextState())
        val parcelsToPut = generateParcelsList(seedGenerator.nextState())
        val location = "loc: " + generateInt(seedGenerator.nextState())
        val body = JsonObject().apply {
            addProperty("dropId", randomDropInfoDTO.dropId)
            addProperty("loc", location)
            add("parcelIds", JsonArray().apply { parcelsToPut.forEach { add(it.parcelId) } })
        }

        val mockedResult = randomDropInfoDTO.copy(
            parcels = randomDropInfoDTO.parcels.orEmpty() + parcelsToPut
        )
        coEvery { droppingApi.putParcelOnDrop(randomToken, body) } returns mockedResult

        val expectedResult = dropInfoParser.parseToEntity(mockedResult)
        assertEquals(expectedResult, droppingRepository.putParcelOnDrop(randomToken, randomDropInfoDTO.dropId.orEmpty(), location, parcelsToPut))

        coVerify { droppingApi.putParcelOnDrop(randomToken, body) }
    }

    @Test
    fun `Test move cancelled parcel`() = runTest {
        val randomParcelId = "parcelId: " + generateInt(seedGenerator.nextState())
        val randomContainerId = "containerId: " + generateInt(seedGenerator.nextState())
        val body = JsonObject().apply {
            addProperty("parcelId", randomParcelId)
            addProperty("containerId", randomContainerId)
        }

        droppingRepository.moveCancelledParcel(randomToken, randomParcelId, randomContainerId)

        coVerify { droppingApi.moveCancelledParcel(randomToken, body) }
    }

    @Test
    fun `Test confirm fixed`() = runTest {
        val randomParcelId = "parcelId: " + generateInt(seedGenerator.nextState())
        val randomUserName = "username: " + generateInt(seedGenerator.nextState())
        val body = JsonObject().apply {
            addProperty("parcelId", randomParcelId)
        }

        droppingRepository.confirmFixed(randomUserName, randomParcelId, randomToken)

        coVerify { confirmFixedApi.confirmFixed(randomToken, randomUserName, body) }
    }

    @Test
    fun `Test validate location`() = runTest {
        val randomLocation = "loc: " + generateInt(seedGenerator.nextState())
        val randomLocationType = "locType: " + generateInt(seedGenerator.nextState())
        val body = JsonObject().apply {
            addProperty("loc", randomLocation)
            addProperty("locType", randomLocationType)
        }

        droppingRepository.validateLocation(randomToken, randomLocation, randomLocationType)

        coVerify { validationApi.validateLocation(randomToken, body) }
    }

    @Test
    fun `Test getting parcel error`() = runTest {
        val randomParcelErrorDTO = generateParcelErrorDTO(seedGenerator.nextState())

        coEvery { droppingApi.parcelError(randomToken) } returns randomParcelErrorDTO

        val expectedResult = parcelErrorParser.parseToEntity(randomParcelErrorDTO)
        assertEquals(expectedResult, droppingRepository.getParcelError(randomToken))

        coVerify { droppingApi.parcelError(randomToken) }
    }
}
