package ru.yandex.market.abo.core.resupply.registry;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.api.entity.resupply.registry.RegistryType;
import ru.yandex.market.abo.core.resupply.registry.exception.RegistryValidationException;
import ru.yandex.market.ff.client.dto.ActDataRowDTO;
import ru.yandex.market.ff.client.enums.UnredeemedPrimaryDivergenceType;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static ru.yandex.market.abo.core.resupply.registry.utils.RegistryUtils.UNREDEEMED_MASK;

@ContextConfiguration("classpath:registry-request-service-test-mocks.xml")
public class RegistryRequestServiceIntegrationTest extends EmptyTest {


    @Autowired
    private RegistryValidationService registryValidationService;
    @Autowired
    private RegistryRequestService registryRequestService;
    @Autowired
    private RegistryRepo registryRepo;
    @Autowired
    private RegistryItemRepo registryItemRepo;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private RegistrySendingService registrySendingService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Registry registry;
    private Registry refundRegistry;

    @BeforeEach
    public void init() {
        registry = new Registry();
        registry.setType(RegistryType.UNPAID);
        registry.setName("Name");
        registry.setDate(LocalDate.now());
        refundRegistry = new Registry();
        refundRegistry.setType(RegistryType.REFUND);
        refundRegistry.setName("ReturnName");
        refundRegistry.setDate(LocalDate.now());
    }

    @AfterEach
    public void resetMocks() {
        Mockito.reset(registryValidationService);
        jdbcTemplate.execute("TRUNCATE TABLE resupply_registry, resupply_registry_item");
    }

    @Test
    public void doNothingWhenRegistryAlreadyHasRequestIdFromFF() {
        registry.setRequestId(111L);
        uploadToRegistryIfNotExists(registry);
        Registry found = registryRepo.findOneByName(registry.getName());

        registryRequestService.sendRegistry(found.getId());
        Registry updatedFound = registryRepo.findOneByName(registry.getName());
        assertThat(updatedFound.getRequestId()).isEqualTo(111L);
    }

    @Test
    public void uploadWithoutErrors() throws IOException {
        registry.setType(RegistryType.REFUND);
        registryRequestService.upload(registry, getFile("correct-registry.txt"), "testUserName");
        assertSuccessfulWriteToDatabase();
    }

    @Test
    public void uploadWithoutOrdersWithoutErrors() throws IOException {
        registry.setType(RegistryType.REFUND);
        registryRequestService.upload(registry, getFile("registry-with-no-order-ids.txt"), "testUserName");
        assertSuccessfulWriteToDatabaseWithoutOrderIds();
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    public void shouldntUploadRegistryTwice() throws IOException {
        registry.setType(RegistryType.REFUND);
        registryRequestService.upload(registry, getFile("correct-registry.txt"), "testUserName");
        assertSuccessfulWriteToDatabase();
        try {
            Registry registry2 = new Registry();
            registry2.setType(RegistryType.UNPAID);
            registry2.setName("Name");
            registry2.setDate(LocalDate.now());

            registryRequestService.upload(registry2, getFile("correct-registry.txt"), "testUserName");
            assertSuccessfulWriteToDatabase();
            fail("Shouldn't upload registry twice");
        } catch (RegistryValidationException e) {
            // pass
            assertSuccessfulWriteToDatabase();
        }
    }

    @Test
    public void uploadWithErrorAfterSave() {
        doThrow(new RuntimeException()).when(registryValidationService).validate(anyList());
        uploadCorrectRegistryInNewTransactionWaitingForValidationException();
        assertNotAddedToDatabase(registry);
    }

    @Test
    public void uploadRefundWithoutTrackCode() {
        assertCorrectExceptionThrown(RegistryValidationException.class,
                () -> registryRequestService.upload(refundRegistry, getFile("refund-registry-without-track-code.txt")
                        , "testUserName"),
                "У некоторых заказов реестра отсутствует треккод.");
        assertNotAddedToDatabase(refundRegistry);
    }

    @Test
    public void uploadWithDuplicatedOrders() {
        assertCorrectExceptionThrown(RegistryValidationException.class,
                () -> registryRequestService.upload(refundRegistry, getFile("registry-with-duplicated-orders.txt"),
                        "testUserName"),
                "Некоторые заказы в реестре дублируются: 1, 12345679");
        assertNotAddedToDatabase(refundRegistry);
    }

    @Test
    public void uploadWithTooLongRegistryNameMore20Symbols() {
        registry.setName("TooLongNameWithMoreThan20Symbols");
        assertCorrectExceptionThrown(RegistryValidationException.class,
                () -> registryRequestService.upload(registry, getFile("correct-registry.txt"), "testUserName"),
                "Длина имени реестра не должна быть пустой или превышать 20 символов");
    }

    @Test
    public void uploadRefundWithUnredeemed() {
        assertCorrectExceptionThrown(RegistryValidationException.class,
                () -> registryRequestService.upload(refundRegistry, getFile("refund-registry-with-unredeemed.txt"),
                        "testUserName"),
                "Некоторые товары являются невыкупами с маской: " + UNREDEEMED_MASK);
        assertNotAddedToDatabase(refundRegistry);
    }

    @Test
    public void uploadWithRegistryNameLengthExactly20Symbols() throws IOException {
        registry.setName("Exactly20SymbolsName");
        registry.setType(RegistryType.REFUND);
        registryRequestService.upload(registry, getFile("correct-registry.txt"), "testUserName");
        assertSuccessfulWriteToDatabase();
    }

    @Test
    public void uploadWithNullRegistryName() {
        registry.setName(null);
        assertCorrectExceptionThrown(RegistryValidationException.class,
                () -> registryRequestService.upload(registry, getFile("correct-registry.txt"), "testUserName"),
                "Длина имени реестра не должна быть пустой или превышать 20 символов");
    }

    @Test
    public void uploadWithEmptyRegistryName() {
        registry.setName("");
        assertCorrectExceptionThrown(RegistryValidationException.class,
                () -> registryRequestService.upload(registry, getFile("correct-registry.txt"), "testUserName"),
                "Длина имени реестра не должна быть пустой или превышать 20 символов");
    }

    @Test
    public void createActDataRowsForRefundWithNullTrackCode() {
        assertRefundFileGenerationWithoutTrackCode("nullTrackCode", null);
    }

    @Test
    public void createActDataRowsForRefundWithEmptyTrackCode() {
        assertRefundFileGenerationWithoutTrackCode("emptyTrackCode", "");
    }

    @Test
    public void createActDataRowsForRefundWithSpaceTrackCode() {
        assertRefundFileGenerationWithoutTrackCode("spaceTrackCode", " ");
    }

    private void assertRefundFileGenerationWithoutTrackCode(String registryName, String emptyTrackCode) {
        Registry refundWithoutTrackCode = new Registry();
        refundWithoutTrackCode.setType(RegistryType.REFUND);
        refundWithoutTrackCode.setName(registryName);
        refundWithoutTrackCode.setDate(LocalDate.now());
        Registry savedRegistry = registryRepo.saveAndFlush(refundWithoutTrackCode);
        RegistryItem firstItem = new RegistryItem();
        firstItem.setOrderId("EXT12345");
        firstItem.setTrackCode(emptyTrackCode);
        firstItem.setRegistry(refundWithoutTrackCode);
        RegistryItem secondItem = new RegistryItem();
        secondItem.setOrderId("123456");
        secondItem.setTrackCode("trackSecond");
        secondItem.setRegistry(refundWithoutTrackCode);
        registryItemRepo.saveAndFlush(firstItem);
        registryItemRepo.saveAndFlush(secondItem);
        List<ActDataRowDTO> actDataRows = registryRequestService.createActDataRows(savedRegistry);
        Assertions.assertEquals(2, actDataRows.size());
        ActDataRowDTO firstRow = actDataRows.get(0);
        ActDataRowDTO secondRow = actDataRows.get(1);
        assertActDataRowExpected(firstRow, "EXT12345", "<отсутствует>",
                UnredeemedPrimaryDivergenceType.NOT_SUPPLIED, 1);
        assertActDataRowExpected(secondRow, "123456", "trackSecond", UnredeemedPrimaryDivergenceType.NOT_SUPPLIED, 1);
    }

    private MultipartFile getFile(String fileName) throws IOException {
        InputStream is = getClass().getResourceAsStream("/resupply/registry/" + fileName);
        return new MockMultipartFile(fileName, is);
    }

    private void assertSuccessfulWriteToDatabase() {
        Registry savedRegistry = registryRepo.findOneByName(registry.getName());
        List<RegistryItem> allItemsForRegistry = registryItemRepo.findAllByRegistryId(savedRegistry.getId());
        Assertions.assertEquals(2, allItemsForRegistry.size());
        List<OrderIdAndTrackCodePair> actualOrderIdAndTrackCodesForRegistry = allItemsForRegistry.stream()
                .map(item -> new OrderIdAndTrackCodePair(item.getOrderId(), item.getTrackCode()))
                .collect(Collectors.toList());
        List<OrderIdAndTrackCodePair> expectedItemsForRegistry = List.of(
                new OrderIdAndTrackCodePair("12345678", "track1"),
                new OrderIdAndTrackCodePair("12345670", "track2")
        );

        for (OrderIdAndTrackCodePair orderIdAndTrackCodePair : expectedItemsForRegistry) {
            Assertions.assertTrue(actualOrderIdAndTrackCodesForRegistry.contains(orderIdAndTrackCodePair));
        }
        Assertions.assertEquals("testUserName", savedRegistry.getUserName());
    }

    private void assertSuccessfulWriteToDatabaseWithoutOrderIds() {
        Registry savedRegistry = registryRepo.findOneByName(registry.getName());
        List<RegistryItem> allItemsForRegistry = registryItemRepo.findAllByRegistryId(savedRegistry.getId());
        Assertions.assertEquals(2, allItemsForRegistry.size());
        List<OrderIdAndTrackCodePair> actualOrderIdAndTrackCodesForRegistry = allItemsForRegistry.stream()
                .map(item -> new OrderIdAndTrackCodePair(item.getOrderId(), item.getTrackCode()))
                .collect(Collectors.toList());
        List<OrderIdAndTrackCodePair> expectedItemsForRegistry = List.of(
                new OrderIdAndTrackCodePair("", "track1"),
                new OrderIdAndTrackCodePair("", "track2")
        );

        for (OrderIdAndTrackCodePair orderIdAndTrackCodePair : expectedItemsForRegistry) {
            Assertions.assertTrue(actualOrderIdAndTrackCodesForRegistry.contains(orderIdAndTrackCodePair));
        }
        Assertions.assertEquals("testUserName", savedRegistry.getUserName());
    }

    private void assertNotAddedToDatabase(Registry registry) {
        Registry savedRegistry = registryRepo.findOneByName(registry.getName());
        Assertions.assertNull(savedRegistry);
    }

    private void uploadCorrectRegistryInNewTransactionWaitingForValidationException() {
        var requiresNewTransactionTemplate = new TransactionTemplate();
        requiresNewTransactionTemplate.setTransactionManager(transactionManager);
        requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        requiresNewTransactionTemplate.afterPropertiesSet();
        try {
            requiresNewTransactionTemplate.execute(status -> {
                try {
                    Registry upload = registryRequestService.upload(registry, getFile("correct-registry.txt"),
                            "testUserName");
                    registryRequestService.validateItems(upload);
                    Assertions.fail("It should be validation exception");
                    return null;
                } catch (IOException e) {
                    Assertions.fail("It should not be IOException");
                } catch (RegistryValidationException e) {
                    Assertions.assertEquals(
                            "Техническая ошибка при валидации реестра. Попробуйте загрузить реестр позже.",
                            e.getMessage()
                    );
                    throw e;
                }
                return null;
            });
        } catch (Exception ignored) {
        }
    }

    private void assertActDataRowExpected(ActDataRowDTO row, String orderId, String trackId,
                                          UnredeemedPrimaryDivergenceType divergenceDescr,
                                          Integer boxesAmountWithDivergence) {
        Assertions.assertEquals(orderId, row.getOrderId());
        Assertions.assertEquals(trackId, row.getTrackId());
        Assertions.assertEquals(divergenceDescr, row.getDivergenceDescr());
        Assertions.assertEquals(boxesAmountWithDivergence, row.getBoxesAmountWithDivergence());
    }

    private void uploadToRegistryIfNotExists(Registry registry) {
        Registry savedRegistry = registryRepo.findOneByName(registry.getName());
        if (savedRegistry == null) {
            registryRepo.saveAndFlush(registry);
        }
    }

    private <T extends Throwable> void assertCorrectExceptionThrown(Class<T> expectedType,
                                                                    Executable executable,
                                                                    String expectedMessage) {
        T actualException = Assertions.assertThrows(expectedType, executable);
        Assertions.assertEquals(expectedMessage, actualException.getMessage());
    }

    private static class OrderIdAndTrackCodePair {
        private final String orderId;
        private final String trackCode;

        private OrderIdAndTrackCodePair(String orderId, String trackCode) {
            this.orderId = orderId;
            this.trackCode = trackCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            OrderIdAndTrackCodePair that = (OrderIdAndTrackCodePair) o;
            return Objects.equals(orderId, that.orderId) &&
                    Objects.equals(trackCode, that.trackCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(orderId, trackCode);
        }
    }
}
