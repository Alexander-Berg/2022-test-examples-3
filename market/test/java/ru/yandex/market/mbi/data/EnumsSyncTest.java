package ru.yandex.market.mbi.data;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.api.cpa.yam.entity.RequestType;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.contact.InnerRole;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.mbi.data.util.ContactRoleConverter;
import ru.yandex.market.mbi.data.util.PartnerAppStatusConverter;
import ru.yandex.market.mbi.data.util.PartnerAppTypeConverter;
import ru.yandex.market.mbi.data.util.PartnerPlacementProgramTypeConverter;
import ru.yandex.market.mbi.data.util.PartnerTypeConverter;

/**
 * Тест синхронизации enum'ов из /trunk/arcadia/market/mbi/proto/mbi_data_proto
 */
public class EnumsSyncTest {
    /**
     * Типы, которых нет у нас в базе, но есть в коде для обратной совместимости.
     */
    private final Set<RequestType> IGNORED_TYPES = EnumSet.of(RequestType.SHOP, RequestType.SUPPLIER);

    /**
     * Тест синхронизации enum'ов {@link PartnerApplicationStatus}.
     * Проверяет, что все enum'ы из {@link PartnerApplicationStatus} добавлены в протобуф
     * /trunk/arcadia/market/mbi/proto/mbi_data_proto/PartnerAppData.proto
     */
    @Test
    void testPartnerAppStatusEnumSync() {
        Arrays.stream(PartnerApplicationStatus.values()).forEach(status -> {
            PartnerAppDataOuterClass.PartnerAppStatus protoStatus =
                    PartnerAppStatusConverter.convertStatusToProto(status);
            Assertions.assertNotEquals(protoStatus, PartnerAppDataOuterClass.PartnerAppStatus.UNKNOWN_APP_STATUS);
            Assertions.assertNotEquals(protoStatus, PartnerAppDataOuterClass.PartnerAppStatus.UNRECOGNIZED);
            Assertions.assertEquals(status, PartnerAppStatusConverter.convertStatusFromProto(protoStatus));
        });
    }

    /**
     * Тест синхронизации enum'ов {@link RequestType}.
     * Проверяет, что все enum'ы из {@link RequestType} добавлены в протобуф
     * /trunk/arcadia/market/mbi/proto/mbi_data_proto/PartnerAppData.proto
     */
    @Test
    void testRequestTypeEnumSync() {
        Arrays.stream(RequestType.values()).filter(type -> !IGNORED_TYPES.contains(type)).forEach(type -> {
            PartnerAppDataOuterClass.PartnerAppType protoType =
                    PartnerAppTypeConverter.convertTypeToProto(type);
            Assertions.assertNotEquals(protoType, PartnerAppDataOuterClass.PartnerAppType.UNKNOWN_APP_TYPE);
            Assertions.assertNotEquals(protoType, PartnerAppDataOuterClass.PartnerAppType.UNRECOGNIZED);
            Assertions.assertEquals(type, PartnerAppTypeConverter.convertTypeFromProto(protoType));
        });
    }

    /**
     * Тест синхронизации enum'ов {@link ru.yandex.market.core.contact.InnerRole}.
     * Проверяет, что все enum'ы из {@link ru.yandex.market.core.contact.InnerRole} добавлены в протобуф
     * /trunk/arcadia/market/mbi/proto/mbi_data_proto/ContactData.proto
     */
    @Test
    void testContactRoleSync() {
        Arrays.stream(InnerRole.values()).forEach(role -> {
            ContactDataOuterClass.ContactRole protoRole = ContactRoleConverter.convertRoleToProto(role);
            Assertions.assertNotEquals(protoRole, ContactDataOuterClass.ContactRole.UNKNOWN_ROLE);
            Assertions.assertNotEquals(protoRole, ContactDataOuterClass.ContactRole.UNRECOGNIZED);
            Assertions.assertEquals(role, ContactRoleConverter.convertRoleFromProto(protoRole));
        });
    }

    /**
     * Тест синхронизации enum'ов {@link ru.yandex.market.core.campaign.model.CampaignType}.
     * Проверяет, что все enum'ы из {@link ru.yandex.market.core.campaign.model.CampaignType} добавлены в протобуф
     * /trunk/arcadia/market/mbi/proto/mbi_data_proto/PartnerData.proto
     */
    @Test
    void testPartnerTypeSync() {
        Arrays.stream(CampaignType.values()).forEach(type -> {
            PartnerDataOuterClass.PartnerType protoType = PartnerTypeConverter.convertPartnerTypeToProto(type);
            Assertions.assertNotEquals(protoType, ContactDataOuterClass.ContactRole.UNKNOWN_ROLE);
            Assertions.assertNotEquals(protoType, ContactDataOuterClass.ContactRole.UNRECOGNIZED);
            Assertions.assertEquals(type, PartnerTypeConverter.convertPartnerTypeFromProto(protoType));
        });
    }

    /**
     * Тест синхронизации enum'ов {@link PartnerPlacementProgramType}.
     * Проверяет, что все enum'ы из {@link PartnerPlacementProgramType} добавлены в протобуф
     * /trunk/arcadia/market/mbi/proto/mbi_data_proto/PartnerData.proto
     */
    @Test
    void testPartnerPlacementProgramTypeSync() {
        Arrays.stream(PartnerPlacementProgramType.values()).forEach(placementProgramType -> {
            PartnerDataOuterClass.PlacementProgramType protoType =
                    PartnerPlacementProgramTypeConverter.convertToProto(placementProgramType);
            Assertions.assertNotEquals(protoType, PartnerDataOuterClass.PlacementProgramType.UNKNOWN_PROGRAM);
            Assertions.assertEquals(placementProgramType,
                    PartnerPlacementProgramTypeConverter.convertFromProto(protoType));
        });
    }
}
