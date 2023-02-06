package ru.yandex.market.fintech.banksint.service.installment.protobuf;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.mds.s3.client.util.TempFileUtils;
import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.installment.InstallmentOptions;
import ru.yandex.market.fintech.banksint.mybatis.CommonPropertyMapper;
import ru.yandex.market.fintech.banksint.mybatis.installment.InstallmentGroupMapper;
import ru.yandex.market.fintech.banksint.service.CategoryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.fintech.banksint.service.installment.protobuf.InstallmentProtobufService.DISABLE_UPLOAD_INSTALLMENTS_PROP_NAME;


class InstallmentProtobufServiceTest extends FunctionalTest {

    private static final int OPTIONS_COUNT = 4; // 3 for each shop + BNPL

    private static final Map<Set<Integer>, List<Long>> SHOP_201_SKU_GROUPS_MAP = Map.of(
            Set.of(180, 360), List.of(2L),
            Set.of(180, 720), List.of(3L, 4L),
            Set.of(180, 360, 720), List.of(5L)
    );

    private int shopsCount = -1;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InstallmentProtobufService uploadService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CommonPropertyMapper commonPropertyMapper;

    @Autowired
    private InstallmentGroupMapper installmentGroupMapper;

    @BeforeEach
    void setUpDatabase() {
        jdbcTemplate.execute(readClasspathFile("SetUpInstallments.sql"));
        shopsCount = installmentGroupMapper.findDistinctShopIds().size();
    }

    @Test
    void testDbFetch() {
        Set<Long> shopIdsSet = new HashSet<>();
        shopIdsSet.add(101L);
        shopIdsSet.add(102L);
        shopIdsSet.add(103L);
        shopIdsSet.add(201L);
        // shops -98 and -99 should not be present

        byte[] serialized = uploadService.downloadStorage().toByteArray();
        AtomicReference<InstallmentOptions.InstallmentOptionsStorage> deserialized = new AtomicReference<>();
        assertDoesNotThrow(() -> deserialized.set(InstallmentOptions.InstallmentOptionsStorage.parseFrom(serialized)));
        var storage = deserialized.get();

        validateVersion(storage); //20211123_1642
        for (var shopOps : storage.getShopsOptionsList()) {
            long shopId = shopOps.getShopId();
            assertFalse(shopOps.getOptionsList().isEmpty(), shopId + " is empty");
            assertEquals(OPTIONS_COUNT, shopOps.getOptionsList().size(), shopId + " does not match");

            checkBnplEnabledForAll(shopOps);
            if (shopId == 201) {
                for (var option : shopOps.getOptionsList()) {
                    final Set<Integer> durations = new HashSet<>(option.getInstallmentTimeInDaysList());
                    if (durations.isEmpty()) { // BNPL group
                        continue;
                    }
                    final List<Long> skus = SHOP_201_SKU_GROUPS_MAP.get(durations);
                    assertTrue(
                            option.getSkuListList().stream()
                            .map(InstallmentOptions.ShopSku::getMarketSku)
                            .allMatch(skus::contains)
                    );
                }
            }


            assertTrue(shopIdsSet.remove(shopId));
        }

        assertTrue(shopIdsSet.isEmpty());

        assertThat(storage.getCategoriesAncestorsCount())
                .isEqualTo(categoryService.getCategoryTree().getCategoryMap().size());
    }

    @Test
    public void uploadEmptyFile() {
        commonPropertyMapper.insertOrUpdateProperty(DISABLE_UPLOAD_INSTALLMENTS_PROP_NAME, "true");

        byte[] serialized = uploadService.downloadStorage().toByteArray();
        AtomicReference<InstallmentOptions.InstallmentOptionsStorage> deserialized = new AtomicReference<>();
        assertDoesNotThrow(() -> deserialized.set(InstallmentOptions.InstallmentOptionsStorage.parseFrom(serialized)));
        var storage = deserialized.get();

        validateVersion(storage); //20211123_1642
        assertThat(storage.getCategoriesAncestorsCount()).isEqualTo(0);
        assertThat(storage.getShopsOptionsCount()).isEqualTo(0);
    }

    @Test
    public void testSkuOptionsFetch() {

    }

    private void validateVersion(InstallmentOptions.InstallmentOptionsStorage storage) {
        assertThat(storage.getVersion()).matches(Pattern.compile("\\d{8}_\\d{4}"));
    }

    private void checkBnplEnabledForAll(InstallmentOptions.ShopInstallmentOptions shopOps) {
        var bnplOptions = shopOps.getOptionsList().stream()
                .filter(InstallmentOptions.InstallmentOptionsGroup::getBnplAvailable)
                .collect(Collectors.toSet());
        assertThat(bnplOptions.size()).isEqualTo(1);
        var bnplOption = bnplOptions.iterator().next();
        assertThat(bnplOption.getGroupName()).isEqualTo("_A_L_L_BNPL");
        assertThat(bnplOption.getCategoriesCount()).isGreaterThan(0);
        assertThat(bnplOption.getInstallmentTimeInDaysList()).isEmpty();
    }

    @Test
    void testDelimitedSerialization() {
        Set<Long> shopIdsSet = new HashSet<>();
        shopIdsSet.add(101L);
        shopIdsSet.add(102L);
        shopIdsSet.add(103L);
        shopIdsSet.add(201L);
        // shops -98 and -99 should not be present

        File tempFile = null;
        try {
            tempFile = TempFileUtils.createTempFile();
            uploadService.downloadDelimitedStoragesToProtofile(tempFile, 1);
            var storagesList = uploadService.parseDelimitedStorages(tempFile);

            assertEquals(shopsCount, storagesList.size());

            assertTrue(storagesList.get(0).hasVersion());
            assertTrue(storagesList.get(0).getCategoriesAncestorsCount() > 0);

            for (int i = 1; i < storagesList.size(); i++) {
                assertFalse(storagesList.get(i).hasVersion());
                assertFalse(storagesList.get(i).getCategoriesAncestorsCount() > 0);
            }

            for (var storage : storagesList) {
                assertEquals(1, storage.getShopsOptionsCount());
                var shopOps = storage.getShopsOptions(0);
                long shopId = shopOps.getShopId();
                assertFalse(shopOps.getOptionsList().isEmpty(), shopId + " is empty");
                assertEquals(OPTIONS_COUNT, shopOps.getOptionsList().size(), shopId + " does not match");

                checkBnplEnabledForAll(shopOps);
                if (shopId == 201) {
                    for (var option : shopOps.getOptionsList()) {
                        final Set<Integer> durations = new HashSet<>(option.getInstallmentTimeInDaysList());
                        if (durations.isEmpty()) { // BNPL group
                            continue;
                        }
                        final List<Long> skus = SHOP_201_SKU_GROUPS_MAP.get(durations);
                        assertTrue(
                                option.getSkuListList().stream()
                                        .map(InstallmentOptions.ShopSku::getMarketSku)
                                        .allMatch(skus::contains)
                        );
                    }
                }


                assertTrue(shopIdsSet.remove(shopId));
            }

        } catch (IOException ioEx) {
            ioEx.printStackTrace();
            throw new RuntimeException(ioEx);
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }

    @Test
    void testBatchSizeHalf() {
        File tempFile = null;
        try {
            tempFile = TempFileUtils.createTempFile();
            int half = shopsCount / 2 + 1;
            uploadService.downloadDelimitedStoragesToProtofile(tempFile, half);
            var storagesList = uploadService.parseDelimitedStorages(tempFile);

            assertEquals(2, storagesList.size());
            assertEquals(half, storagesList.get(0).getShopsOptionsCount());
            assertEquals(shopsCount - half, storagesList.get(1).getShopsOptionsCount());

        } catch (IOException ioEx) {
            ioEx.printStackTrace();
            throw new RuntimeException(ioEx);
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }

    @Test
    void testBatchSizeFull() {
        File tempFile = null;
        try {
            tempFile = TempFileUtils.createTempFile();
            uploadService.downloadDelimitedStoragesToProtofile(tempFile, shopsCount);
            var storagesList = uploadService.parseDelimitedStorages(tempFile);

            assertEquals(1, storagesList.size());
            assertEquals(shopsCount, storagesList.get(0).getShopsOptionsCount());

        } catch (IOException ioEx) {
            ioEx.printStackTrace();
            throw new RuntimeException(ioEx);
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }

}
