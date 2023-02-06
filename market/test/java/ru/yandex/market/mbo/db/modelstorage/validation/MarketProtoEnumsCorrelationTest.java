package ru.yandex.market.mbo.db.modelstorage.validation;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.protobuf.ProtocolMessageEnum;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.domain.Sort;

import ru.yandex.market.ir.http.Classifier;
import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageProtoConverters;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.gurulight.OfferDataMatchType;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.GeneralizationStrategy;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.MappingProcessingStatus;
import ru.yandex.market.mbo.gwt.models.param.ModifiedState;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.tovartree.NameToAliasesSettings;
import ru.yandex.market.mbo.gwt.models.transfer.step.ClassifierReloadRequestEntry;
import ru.yandex.market.mbo.gwt.models.vendor.LogoPosition;
import ru.yandex.market.mbo.gwt.models.vendor.LogoType;
import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mbo.http.MboGuruService;
import ru.yandex.market.mbo.http.MboVendors;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.http.SupplierOffer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, что внутренние типы соответствуют типам из маркет прото.
 * Если у вас начали падать тесты, то добавьте недостающие константы.
 * Если в МБО нет констант, то надо их добавить в МБО.
 * Если в маркет прото нет констант, то вы забыли обновить либу.
 *
 * @author s-ermakov
 */
public class MarketProtoEnumsCorrelationTest {

    @Test
    public void testRelationType() {
        testEnumCorrelation(ModelRelation.RelationType.class, ModelStorage.RelationType.class);
    }

    @Test
    public void testParameterValueType() {
        testEnumCorrelation(Param.Type.class, ModelStorage.ParameterValueType.class);
    }

    @Test
    public void testModificationSource() {
        testEnumCorrelation(ModificationSource.class, ModelStorage.ModificationSource.class);
    }

    @Test
    public void testOperationStatusType() {
        testEnumCorrelation(OperationStatusType.class, ModelStorage.OperationStatusType.class);
    }

    @Test
    public void testOperationType() {
        testEnumCorrelation(OperationType.class, ModelStorage.OperationType.class);
    }

    @Test
    public void testModifiedState() {
        testEnumCorrelation(ModifiedState.class, MboParameters.ModificationState.class);
    }

    @Test
    public void testErrorType() {
        testEnumCorrelation(ModelValidationError.ErrorType.class, ModelStorage.ValidationErrorType.class);
    }

    //TODO - test didn't start in github, but fails after moving to arcadia, fix it later
    @Test
    @Ignore
    public void testErrorSubType() {
        testEnumCorrelation(ModelValidationError.ErrorSubtype.class, ModelStorage.ValidationErrorSubtype.class);
    }

    @Test
    public void testGlobalVendorLogoType() {
        testEnumCorrelation(LogoType.class, MboVendors.LogoType.class);
    }

    @Test
    public void testGlobalVendorLogoPosition() {
        testEnumCorrelation(LogoPosition.class, MboVendors.Position.class);
    }

    @Test
    public void testAuditActionEntityType() {
        //!!!!!!!!!!!!!!!!!!!!
        // ATTENTION! Manual ClickHouse update is required in case enum is changed
        // (this enum is in key and current ddl lib doesn't support such updates, CH does).
        // (used in mbo-audit, ru.yandex.market.mbo.audit.AuditTableConfigurationService)
        //!!!!!!!!!!!!!!!!!!!!

        // Don't test in reverse yet as there are extra items in proto to come later
        testJavaEnumsContainsInProto(AuditAction.EntityType.class, MboAudit.EntityType.class);
    }

    @Test
    public void testAuditActionActionType() {
        //!!!!!!!!!!!!!!!!!!!!
        // ATTENTION! Manual ClickHouse update is required in case enum is changed
        // (this enum is in key and current ddl lib doesn't support such updates, CH does).
        // (used in mbo-audit, ru.yandex.market.mbo.audit.AuditTableConfigurationService)
        //!!!!!!!!!!!!!!!!!!!!

        // Don't test in reverse yet as there are extra items in proto to come later
        testJavaEnumsContainsInProto(AuditAction.ActionType.class, MboAudit.ActionType.class);
    }

    @Test
    public void testChangeStatus() {
        testEnumCorrelation(MappingProcessingStatus.ChangeStatus.class,
            SupplierOffer.Offer.MappingProcessingStatus.ChangeStatus.class);
    }

    @Test
    public void testSupplierType() {
        testEnumCorrelation(SupplierOffer.SupplierType.class,
            SupplierOffer.SupplierType.class);
    }

    @Test
    public void testGeneralizationStrategy() {
        for (ModelStorage.GeneralizationStrategy protoValue : ModelStorage.GeneralizationStrategy.values()) {
            GeneralizationStrategy value = ModelProtoConverter.convert(protoValue);
            assertThat(value).isNotNull();
        }
    }

    @Test
    public void entriesInEnumShouldBeMappedOneToOneToMatcherMatchType() {
        testEnumCorrelation(OfferDataMatchType.class,
            Matcher.MatchType.class);
    }

    @Test
    public void testClassifierReloadStatus() {
        testEnumCorrelation(ClassifierReloadRequestEntry.ReloadStatus.class,
            Classifier.HasReloadFinishedResponse.ReloadStatus.class);
    }

    @Test
    public void testOrderType() {
        testEnumCorrelation(Sort.Direction.class, ModelStorage.FindModelsRequest.OrderType.class);
    }

    @Test
    public void testModelType() {
        for (ModelStorage.ModelType modelType : ModelStorage.ModelType.values()) {
            CommonModel.Source type = ModelStorageProtoConverters.getByModelType(modelType);
        }
    }

    @Test
    public void testNameToAliasType() {
        testEnumCorrelation(NameToAliasesSettings.class, MboGuruService.NameToAliasSetting.class);
    }

    private static <T1 extends Enum<T1>, T2 extends Enum<T2> & ProtocolMessageEnum> void testEnumCorrelation(
        Class<T1> enum1, Class<T2> enum2) {

        List<String> enumNames1 = Arrays.stream(enum1.getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toList());
        List<String> enumNames2 = Arrays.stream(enum2.getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toList());

        assertThat(enumNames2).containsOnlyElementsOf(enumNames1);
    }

    private static <T1 extends Enum<T1>, T2 extends Enum<T2> & ProtocolMessageEnum>
    void testJavaEnumsContainsInProto(Class<T1> javaEnum, Class<T2> protoEnum) {

        List<String> javaEnumNames = Arrays.stream(javaEnum.getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toList());
        List<String> protoEnumNames = Arrays.stream(protoEnum.getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toList());
        assertThat(protoEnumNames).containsAll(javaEnumNames);
    }
}
