package ru.yandex.market.mboc.common.services.category_info.info;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.jooq.repo.OffsetFilter;
import ru.yandex.market.mbo.lightmapper.test.EmptyGenericMapperRepositoryMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepository;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;

/**
 * @author s-ermakov
 */
public class CategoryInfoRepositoryMock extends EmptyGenericMapperRepositoryMock<CategoryInfo, Long>
    implements CategoryInfoRepository {

    private final MboUsersRepository mboUsersRepository;

    public CategoryInfoRepositoryMock(MboUsersRepository mboUsersRepository) {
        super(CategoryInfo::getCategoryId);
        this.mboUsersRepository = mboUsersRepository;
    }

    @Override
    protected void validate(CategoryInfo instance) {
        Long contentManagerUid = instance.getContentManagerUid();
        if (contentManagerUid != null && !containsUser(contentManagerUid)) {
            throw new IllegalStateException(String.format("Content manager (uid %d) should exist before inserting item",
                contentManagerUid));
        }
        Long inputManagerUid = instance.getInputManagerUid();
        if (inputManagerUid != null && !containsUser(inputManagerUid)) {
            throw new IllegalStateException(String.format("Input manager (uid %d) should exist before inserting item",
                contentManagerUid));
        }
    }

    private boolean containsUser(Long uid) {
        List<MboUser> users = mboUsersRepository.findByIds(Collections.singleton(uid));
        return !users.isEmpty();
    }

    @Override
    public Set<Long> filterWithManualAcceptance(Collection<Long> categoryIds) {
        return findByIds(categoryIds).stream()
            .filter(CategoryInfo::isManualAcceptance)
            .map(CategoryInfo::getCategoryId)
            .collect(Collectors.toSet());
    }

    @Override
    public void resetAndSaveAllCategoriesManagers(Collection<CategoryManagers> categoryManagers) {
        Map<Long, CategoryManagers> byId = categoryManagers.stream()
            .collect(Collectors.toMap(CategoryManagers::getCategoryId, Function.identity()));

        List<CategoryInfo> existing = findByIds(byId.keySet());
        for (CategoryInfo categoryInfo : existing) {
            CategoryManagers managers = byId.remove(categoryInfo.getCategoryId());
            categoryInfo.setManagers(managers);
        }

        List<CategoryInfo> newInfos = byId.values().stream()
            .map(m -> {
                CategoryInfo categoryInfo = new CategoryInfo(m.getCategoryId());
                categoryInfo.setManagers(m);
                return categoryInfo;
            })
            .collect(Collectors.toList());

        insertBatch(newInfos);
    }

    @Override
    public Set<Long> findIdsByHideFromToloka(boolean hideFromToloka) {
        return findAll().stream()
            .filter(categoryInfo -> categoryInfo.isHideFromToloka() == hideFromToloka)
            .map(categoryInfo -> categoryInfo.getCategoryId())
            .collect(Collectors.toSet());
    }

    @Override
    public List<CategoryInfo> find(OffsetFilter offsetFilter) {
        return findAll().stream().skip(offsetFilter.getOffset()).limit(offsetFilter.getLimit()).collect(Collectors.toList());
    }

}
