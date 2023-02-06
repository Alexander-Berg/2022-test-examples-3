package ru.yandex.market.mbo.mdm.common.masterdata.repository.param;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MdmGoodGroup;
import ru.yandex.market.mboc.common.masterdata.model.cccode.CustomsCommCode;
import ru.yandex.market.mboc.common.web.CustomsCommCodeLite;

public class CustomsCommCodeRepositoryMock implements CustomsCommCodeRepository {

    private final Map<String, CustomsCommCode> codes = new HashMap<>();

    @Override
    public List<CustomsCommCodeLite> findAllLite(boolean withGroups) {
        return List.of();
    }

    @Override
    public Optional<CustomsCommCode> findByCode(String code) {
        return Optional.ofNullable(codes.get(code));
    }

    @Override
    public void deleteSubTree(long id) {
        String ccode = findById(id).getCode();
        codes.remove(ccode);
        List<Long> children = codes.values().stream()
            .filter(c -> c.getParentId() == id)
            .map(CustomsCommCode::getId)
            .collect(Collectors.toList());
        children.forEach(this::deleteSubTree);
    }

    @Override
    public List<CustomsCommCode> findByGoodGroups(Collection<MdmGoodGroup> goodGroups) {
        List<Long> goodGroupIds = goodGroups.stream().map(MdmGoodGroup::getId).collect(Collectors.toList());
        return codes.values().stream()
            .filter(customsCommCode -> goodGroupIds.contains(customsCommCode.getGoodGroupId()))
            .collect(Collectors.toList());
    }

    @Override
    public CustomsCommCode insert(CustomsCommCode instance) {
        codes.put(instance.getCode(), instance);
        return instance;
    }

    @Override
    public List<CustomsCommCode> insertBatch(Collection<CustomsCommCode> instances) {
        instances.forEach(this::insert);
        return new ArrayList<>(instances);
    }

    @Override
    public CustomsCommCode update(CustomsCommCode customsCommCode) {
        insert(customsCommCode);
        return customsCommCode;
    }

    @Override
    public List<CustomsCommCode> updateBatch(Collection<CustomsCommCode> instances, int batchSize) {
        return instances.stream().map(this::update).collect(Collectors.toList());
    }

    @Override
    public void delete(List<Long> ids) {
        ids.forEach(id -> {
            codes.remove(findById(id).getCode());
        });
    }

    @Override
    public void delete(CustomsCommCode customsCommCode) {
        codes.remove(customsCommCode.getCode());
    }

    @Override
    public void deleteBatch(Collection<CustomsCommCode> customsCommCodes) {
        customsCommCodes.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        codes.clear();
    }

    @Override
    public List<CustomsCommCode> insertOrUpdateAll(Collection<CustomsCommCode> customsCommCodes) {
        insertBatch(customsCommCodes);
        return new ArrayList<>(customsCommCodes);
    }

    @Override
    public Integer insertOrUpdateAllIfDifferent(Collection<CustomsCommCode> customsCommCodes) {
        insertOrUpdateAll(customsCommCodes);
        return customsCommCodes.size();
    }

    @Override
    public CustomsCommCode findById(Long id) {
        return codes.values().stream().filter(c -> c.getId() == id).findFirst().orElse(null);
    }

    @Override
    public CustomsCommCode findByIdForUpdate(Long id) {
        return findById(id);
    }

    @Override
    public List<CustomsCommCode> findByIds(Collection<Long> ids) {
        return ids.stream().map(this::findById).collect(Collectors.toList());
    }

    @Override
    public List<CustomsCommCode> findByIdsForUpdate(Collection<Long> ids) {
        return findByIds(ids);
    }

    @Override
    public List<CustomsCommCode> findAll() {
        return new ArrayList<>(codes.values());
    }

    @Override
    public int totalCount() {
        return codes.size();
    }
}
