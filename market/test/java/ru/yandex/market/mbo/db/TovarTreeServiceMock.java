package ru.yandex.market.mbo.db;

import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class TovarTreeServiceMock extends TovarTreeService {

    private final TovarTreeDao tovarTreeDao;

    private final Map<Long, TMTemplate> templateMap = new HashMap<>();

    public TovarTreeServiceMock() {
        this(Collections.emptyList());
    }

    public TovarTreeServiceMock(TovarTreeDao tovarTreeDao) {
        super(null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null);
        this.tovarTreeDao = tovarTreeDao;
    }

    public TovarTreeServiceMock(TovarCategory... tovarCategories) {
        this(Arrays.asList(tovarCategories));
    }

    public TovarTreeServiceMock(Collection<TovarCategory> tovarCategories) {
        this(new TovarTreeDaoMock(tovarCategories));
    }

    public TovarTreeServiceMock addCategory(TovarCategory category) {
        ((TovarTreeDaoMock) tovarTreeDao).addCategory(category);
        return this;
    }

    public TovarTreeServiceMock addCategories(TovarCategory... categories) {
        return addCategories(Arrays.asList(categories));
    }

    public TovarTreeServiceMock addCategories(Collection<TovarCategory> categories) {
        for (TovarCategory category : categories) {
            addCategory(category);
        }
        return this;
    }

    @Override
    public Map<Long, String> getCategoryNames(Collection<Long> categoryIds) {
        return tovarTreeDao.getCategoryNames(categoryIds);
    }

    @Override
    public List<TovarCategory> loadCategoriesByHids(Collection<Long> hids) {
        return hids.stream().map(this::loadCategoryByHid).collect(Collectors.toList());
    }

    @Override
    public TovarCategory loadCategoryByHid(long hid) {
        return tovarTreeDao.loadCategoryByHid(hid);
    }

    @Override
    public TovarCategory getCategoryByHid(long hid) {
        return tovarTreeDao.getCategoryByHid(hid);
    }

    @Override
    public TovarTree loadTovarTree() {
        return tovarTreeDao.loadTovarTree();
    }

    @Override
    public TovarTree loadTreeScheme() {
        return loadTovarTree();
    }

    public TovarTreeDao getTovarTreeDao() {
        return tovarTreeDao;
    }
}
