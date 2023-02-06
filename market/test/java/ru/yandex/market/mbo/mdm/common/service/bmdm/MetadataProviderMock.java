package ru.yandex.market.mbo.mdm.common.service.bmdm;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.CommonTypeEnumKey;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.MetadataHolder;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.MetadataProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.reference.BmdmExternalReferenceFilter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.reference.ConstantBmdmExternalReferenceProvider;
import ru.yandex.market.mdm.http.MdmBase;
import ru.yandex.market.mdm.http.common_view.CommonEntityTypeEnum;
import ru.yandex.market.mdm.http.common_view.CommonParamViewSetting;
import ru.yandex.market.mdm.http.common_view.CommonViewTypeEnum;

public class MetadataProviderMock implements MetadataProvider {
    private final Map<Long, MdmBase.MdmEntityType> entityTypes = new LinkedHashMap<>();
    private final Map<Long, MdmBase.MdmAttribute> attributes = new LinkedHashMap<>();
    private final Map<CommonTypeEnumKey, List<CommonParamViewSetting>> settings = new LinkedHashMap<>();
    private final MutableBmdmExternalReferenceProvider mutableBmdmExternalReferenceProvider =
        new MutableBmdmExternalReferenceProvider();

    public void addEntityType(MdmBase.MdmEntityType entityType) {
        entityTypes.merge(
            entityType.getMdmId(),
            entityType,
            (a, b) -> {
                throw new RuntimeException("Entity type " + a.getMdmId() + " already exists.");
            }
        );
        for (MdmBase.MdmAttribute attribute : entityType.getAttributesList()) {
            attributes.merge(
                attribute.getMdmId(),
                attribute,
                (a, b) -> {
                    throw new RuntimeException("Attribute " + a.getMdmId() + " already exists.");
                }
            );
        }
    }

    public void addCommonParamViewSetting(CommonParamViewSetting commonParamViewSetting) {
        CommonTypeEnumKey key = new CommonTypeEnumKey(
            commonParamViewSetting.getCommonViewType().getCommonEntityTypeEnum(),
            commonParamViewSetting.getCommonViewType().getViewType());
        settings.merge(
            key,
            List.of(commonParamViewSetting),
            (a, b) -> Stream.of(a, b).flatMap(Collection::stream).collect(Collectors.toList())
        );
    }

    public void addExternalReference(MdmBase.MdmExternalReference mdmExternalReference) {
        mutableBmdmExternalReferenceProvider.addExternalReference(mdmExternalReference);
    }

    public void addExternalReferences(Collection<MdmBase.MdmExternalReference> mdmExternalReferences) {
        mdmExternalReferences.forEach(this::addExternalReference);
    }

    @Override
    public List<MdmBase.MdmAttribute> findAttributes(Collection<Long> attributeIds) {
        return attributeIds.stream()
            .map(this::findAttribute)
            .flatMap(Optional::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public Optional<MdmBase.MdmAttribute> findAttribute(long attributeId) {
        return Optional.ofNullable(attributes.get(attributeId));
    }

    @Override
    public Optional<List<CommonParamViewSetting>> findCommonParamViewSetting(CommonEntityTypeEnum entityTypeEnum,
                                                                             CommonViewTypeEnum viewTypeEnum,
                                                                             boolean onlyEnabled) {
        Optional<List<CommonParamViewSetting>> funcSettings =
            Optional.ofNullable(settings.get(new CommonTypeEnumKey(entityTypeEnum, viewTypeEnum)));
        if (funcSettings.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(funcSettings.get().stream()
            .filter(p -> p.getIsEnabled() == onlyEnabled)
            .collect(Collectors.toList()));
    }

    @Override
    public List<MdmBase.MdmEntityType> findEntityTypes(Collection<Long> entityTypeIds) {
        return entityTypeIds.stream()
            .map(this::findEntityType)
            .flatMap(Optional::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public Optional<MdmBase.MdmEntityType> findEntityType(long entityTypeId) {
        return Optional.ofNullable(entityTypes.get(entityTypeId));
    }

    @Override
    public List<MdmBase.MdmExternalReference> findExternalReferences(BmdmExternalReferenceFilter filter) {
        return mutableBmdmExternalReferenceProvider.findExternalReferences(filter);
    }

    @Override
    public MetadataHolder findAllMetadata() {
        throw new UnsupportedOperationException();
    }

    private static final class MutableBmdmExternalReferenceProvider extends ConstantBmdmExternalReferenceProvider {

        MutableBmdmExternalReferenceProvider() {
            super(List.of());
        }

        public void addExternalReference(MdmBase.MdmExternalReference mdmExternalReference) {
            super.addExternalReference(mdmExternalReference);
        }
    }
}
