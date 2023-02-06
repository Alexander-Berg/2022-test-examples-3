package ru.yandex.market.fintech.banksint.mybatis.installment;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.InstallmentsFileInfo;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.ResourceStatus;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.ResourceType;
import ru.yandex.market.fintech.banksint.util.InstallmentsUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OffersGenerationMapperTest extends FunctionalTest {

    @Autowired
    private OffersGenerationMapper mapper;

    @Autowired
    private JdbcTemplate template;

    @BeforeEach
    void clean() {
        template.execute("truncate table installment_files cascade ");
    }

    @Test
    void testSelectGenerationInfo() {
        template.execute("" +
                "insert into installment_files (resource_id, shop_id, business_id, name, file_type) " +
                "values ('abcd', 123, 234, 'Simple name', 'TEMPLATE')");

        var info = mapper.getInstallmentFileInfoByResourceId("abcd");
        System.out.println(info.toString());
        assertEquals("abcd", info.getResourceId());
        assertEquals(123L, info.getShopId());
        assertEquals(234, info.getBusinessId());
        assertEquals("Simple name", info.getName());
        assertEquals(ResourceStatus.PENDING, info.getStatus());
        assertNull(info.getUrlToDownload());
        assertNull(info.getFailReason());
        assertNotNull(info.getCreatedAt());
        assertNotNull(info.getUpdatedAt());
        assertEquals(ResourceType.TEMPLATE, info.getFileType());

    }

    @Test
    void testInsertNewTask() {
        String id = InstallmentsUtils.generateResourceId();
        var info = InstallmentsFileInfo.builder()
                .setResourceId(id)
                .setShopId(123L)
                .setBusinessId(234L)
                .setName("Simple name")
                .build();

        mapper.insertNewOfferGenerationTask(info);
        var selected = mapper.getInstallmentFileInfoByResourceId(id);
        assertEquals(id, selected.getResourceId());
        assertEquals(ResourceStatus.PENDING, selected.getStatus());
    }

    @Test
    void testGenerationPipeline() {
        String id = InstallmentsUtils.generateResourceId();
        var info = InstallmentsFileInfo.builder()
                .setResourceId(id)
                .setShopId(123L)
                .setBusinessId(234L)
                .setName("Simple name")
                .build();
        mapper.insertNewOfferGenerationTask(info);
        var selected = mapper.getInstallmentFileInfoByResourceId(id);
        assertEquals(id, selected.getResourceId());
        assertEquals(id, selected.getResourceId());
        assertEquals(ResourceStatus.PENDING, selected.getStatus());
        assertNotNull(selected.getCreatedAt());
        assertNotNull(selected.getUpdatedAt());
        assertNull(selected.getUrlToDownload());

        mapper.updateGenerationTaskStatus(id, ResourceStatus.PROCESSING);
        assertEquals(ResourceStatus.PROCESSING, mapper.getTaskStatus(id));

        String url = "www.internet.com";
        mapper.setUrlAndFinishProcessingTask(id, url);
        assertEquals(ResourceStatus.DONE, mapper.getTaskStatus(id));
        selected = mapper.getInstallmentFileInfoByResourceId(id);
        assertEquals(url, selected.getUrlToDownload());
        assertNotEquals(selected.getCreatedAt(), selected.getUpdatedAt());
        assertNull(selected.getFailReason());

        String errorMsg = "heart attack";
        mapper.failAndReportGenerationTask(id, errorMsg);
        assertEquals(ResourceStatus.FAILED, mapper.getTaskStatus(id));
        selected = mapper.getInstallmentFileInfoByResourceId(id);
        assertEquals(errorMsg, selected.getFailReason());


        mapper.resetGenerationTask(id);
        assertEquals(ResourceStatus.PENDING, mapper.getTaskStatus(id));
    }

    @Test
    void testPendingSelection() {

        LocalDateTime now = LocalDateTime.now();
        var builder = InstallmentsFileInfo.builder()
                .setBusinessId(123L)
                .setShopId(234L)
                .setName("just name");
        String id1 = InstallmentsUtils.generateResourceId();
        String id2 = InstallmentsUtils.generateResourceId();
        String id3 = InstallmentsUtils.generateResourceId();
        String id4 = InstallmentsUtils.generateResourceId();

        mapper.insertNewOfferGenerationTask(builder.setResourceId(id1).build());
        mapper.insertNewOfferGenerationTask(builder.setResourceId(id2).build());
        mapper.insertNewOfferGenerationTask(builder.setResourceId(id3).build());
        mapper.insertNewOfferGenerationTask(builder.setResourceId(id4).build());

        mapper.updateGenerationTaskStatus(id3, ResourceStatus.PROCESSING);
        mapper.setUrlAndFinishProcessingTask(id4, "url");

        template.execute("" +
                "insert into installment_files (resource_id, shop_id, business_id, name, file_type, updated_at) " +
                "values ('abcd', 123, 234, 'Simple name', 'INSTALLMENT', '2000-10-10 10:00:00')");

        var selected = mapper.getPendingGenerationTasks("1m", "1m");
        assertTrue(selected.isEmpty());

        template.execute("" +
                "update installment_files set updated_at = '2000-10-15 10:00:00'");

        selected = mapper.getPendingGenerationTasks("1m", "1m");
        assertEquals(3, selected.size());


    }



}
