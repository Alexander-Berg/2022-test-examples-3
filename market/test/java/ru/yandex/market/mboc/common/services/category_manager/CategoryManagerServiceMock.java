package ru.yandex.market.mboc.common.services.category_manager;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.ManagerRole;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Catteam;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;

public
class CategoryManagerServiceMock implements CategoryManagerService {
    @Override
    public List<Long> getBusyCategories() {
        return null;
    }

    @Override
    public List<Long> getAllCategoriesByManager(String login) {
        return null;
    }

    @Override
    public List<Long> getAllCategoriesByManager(ManagerRole managerRole, String login) {
        return null;
    }

    @Override
    public List<Long> getAllCategoriesByCatteam(String catteam) {
        return null;
    }

    @Override
    public List<ManagerCategory> getFullManagerCategoriesBindings() {
        return null;
    }

    @Override
    public Optional<ManagerCategory> getNearestCategoryManagerForCategory(long categoryId) {
        return Optional.empty();
    }

    @Override
    public Map<Long, ManagerCategory> getNearestCategoryManagerForCategories(Collection<Long> categoryIds) {
        return Map.of();
    }

    @Override
    public Map<Long, ManagerCategory> getNearestAssortmentManagerForCategories(Collection<Long> categoryIds) {
        return Map.of();
    }

    @Override
    public List<ManagerCategory> getManagersForCategoryHierarchy(long categoryId) {
        return null;
    }

    @Override
    public List<String> getAllManagers() {
        return null;
    }

    @Override
    public List<String> getAllCatteams() {
        return null;
    }

    @Override
    public List<MbocUserInfo> getAllUsers() {
        return null;
    }

    @Override
    public List<ManagerUserInfo> getAllManagerUsers() {
        return null;
    }

    @Override
    public void updateManagersToCategories(Map<Long, List<ManagerCategory>> categoryToManagers) {

    }

    @Override
    public boolean removeCategoryFromManager(String staffLogin, long categoryId, ManagerRole role) {
        return false;
    }

    @Override
    public void removeAllCategoriesFromManagers() {

    }

    @Override
    public void updateCatteams(List<Catteam> catteamsByCategory) {

    }

    @Override
    public List<Catteam> getCatteams() {
        return null;
    }

    @Override
    public ExcelFile exportAllToExcel() {
        return null;
    }

    @Override
    public boolean importAllFromExcel(ExcelFile excelFile) {
        return false;
    }

    @Override
    public List<ManagerCategory> getManagersForCategories(List<Long> categoryIds) {
        return null;
    }

    @Override
    public Map<Long, String> getCatteamsForCategories(Collection<Long> categoryIds) {
        return null;
    }

    @Override
    public Map<Long, String> getCatDirsForCategories(Collection<Long> categoryIds) {
        return null;
    }

    @Override
    public Map<Long, CategoryInfo> getCategoryInfos() {
        return null;
    }

    @Override
    public Map<Long, CategoryInfo> getCategoryInfosByIds(Collection<Long> ids) {
        return null;
    }

    @Override
    public void updateCategoryInfos(Collection<CategoryInfo> categoryInfos) {

    }
}
