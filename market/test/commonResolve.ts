import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import Api from 'src/Api';
import { testUser } from 'src/test/data/user';
import { DisplayManagerCategory, Warehouse, Supplier, PartnerRelation } from 'src/java/definitions';
import { cargoTypeKGT } from 'src/test/data/displayCargoType';
import { testSuppliers } from 'src/test/data/supplier';
import { testFrontendConfig } from 'src/test/data/frontendConfig';
import { twoTestWarehouses } from 'src/test/data/warehouses';
import { someTestCategories } from 'src/test/data/categories';
import { EMPTY_FRONTEND_VERSIONS } from '../pages/frontendVersions/constants';

type AppApiMock = MockedApiObject<Api>;

export function resolveCategoriesManagerUsers(api: AppApiMock, managers = []) {
  api.managerCategoriesController.getManagerUsers.next().resolve(managers);
}

export function resolveManagerCategoriesAll(api: AppApiMock, managers: DisplayManagerCategory[]) {
  api.managerCategoriesController.all.next().resolve(managers);
}

/**
 * Requests present on all pages.
 */
export function resolveCommonRequests(api: AppApiMock) {
  resolveCurrentUser(api);
  resolveConfig(api);
}

export function resolveCurrentUser(api: AppApiMock) {
  api.currentUserController.getCurrentUser.next().resolve(testUser);
}

export function resolveConfig(api: AppApiMock) {
  api.configController.getConfig.next().resolve(testFrontendConfig());
}

export function resolveCargoTypes(api: AppApiMock) {
  api.cargoTypesController.all.next().resolve([cargoTypeKGT]);
}

export function resolveSuppliers(api: AppApiMock, suppliers: Supplier[] = testSuppliers) {
  api.suppliersController.byFilter.next().resolve({
    items: suppliers,
    totalCount: -1,
  });
}

export function resolvePartnerRelation(api: AppApiMock, partnerRelations: PartnerRelation[] = []) {
  api.partnerRelationController.list.next().resolve(partnerRelations);
}

export function resolveWareHouses(api: AppApiMock, warehouses: Warehouse[] = twoTestWarehouses) {
  api.warehousesController.list.next().resolve(warehouses);
}

export function resolveDeepmindCategoriesAll(api: AppApiMock, categories = someTestCategories) {
  api.deepmindCategoriesController.all.next().resolve(categories);
}

export function resolveCategoriesAll(api: AppApiMock, categories = someTestCategories) {
  api.categoriesController.all.next().resolve(categories);
}

export function resolveFrontendVersions(api: AppApiMock) {
  api.s3FrontendController.getVersions.next().resolve(EMPTY_FRONTEND_VERSIONS);
}
