import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { TestCmApp } from 'src/test/setupApp';
import { ResultAvailabilityPage } from '.';
import { ResultAvailabilityFilterForm } from './components/ResultAvailabilityFilterForm';
import { Link } from 'src/components';
import {
  resolveDeepmindCategoriesAll,
  resolveCommonRequests,
  resolveCategoriesManagerUsers,
  resolveSuppliers,
  resolveWareHouses,
  resolveCargoTypes,
  resolvePartnerRelation,
} from 'src/test/commonResolve';
import {
  testDisplayMsku,
  testDisplayOffer,
  twoTestWarehouses,
  testResultAvailability,
  testSupplier,
} from 'src/test/data/';
import { Reason, ResultAvailability } from 'src/java/definitions';
import Api from 'src/Api';
import { ResultAvailabilityTable } from './components/ResultAvailabilityTable';
import { getShopSkuKeysSelector } from 'src/store/root/result-availability/result-availability.selectors';
import { RESULT_AVAILABILITY_PAGE } from 'src/constants';
import { threeTestSeasons } from 'src/test/data/seasons';
import { SupplierRow } from './components/ResultAvailabilityTable/SupplierRow';

function resolveResultSskuRequest(api: MockedApiObject<Api>, ssku: ResultAvailability) {
  api.resultSskuAvailabilityController.listWithBigFilter.next().resolve([ssku]);
}

function resolveSessionIdRequest(api: MockedApiObject<Api>) {
  api.resultSskuAvailabilityController.getIndexSessionId.next().resolve('17.03.2020 09:15');
}

function resolveHidingsSubReasonRequest(api: MockedApiObject<Api>) {
  api.resultSskuAvailabilityController.hidingsSubReasonDescription.next().resolve([]);
}

function resolveGetCategoriesByManager(api: MockedApiObject<Api>) {
  api.managerCategoriesController.getCategoriesByManager.next().resolve([]);
}

function resolveSeasonalDictionary(api: MockedApiObject<Api>) {
  api.seasonalMskuController.dictionary.next().resolve([]);
}

function resolveSeasonalMskuIdsList(api: MockedApiObject<Api>) {
  api.seasonalMskuController.list.next().resolve([]);
}

function resolveCatTeamsRequiest(api: MockedApiObject<Api>) {
  api.managerCategoriesController.getCatteams.next().resolve([]);
}

function resolveAssortSskuRequest(api: MockedApiObject<Api>) {
  api.assortSskuController.getByKeys.next().resolve([]);
}

const PAGE_URL = RESULT_AVAILABILITY_PAGE;

describe('<ResultAvailabilityPage />', () => {
  jest.useFakeTimers();

  function resolveSskuRequests(api: MockedApiObject<Api>) {
    resolveCommonRequests(api);

    resolveWareHouses(api);
    resolveWareHouses(api);
    resolveDeepmindCategoriesAll(api);
    api.seasonController.list.next().resolve({ items: threeTestSeasons, totalCount: 0 });
    resolveCategoriesManagerUsers(api);
    resolveSessionIdRequest(api);
    resolveHidingsSubReasonRequest(api);
    resolveGetCategoriesByManager(api);
    resolveCargoTypes(api);
    resolveCatTeamsRequiest(api);
  }

  it('renders ssku page correctly', () => {
    const app = new TestCmApp(`${PAGE_URL}?categoryManagerLogin=manager&hidingSubReasons=45k`);
    expect(app.find(ResultAvailabilityPage)).toHaveLength(1);

    app.find(ResultAvailabilityPage).prop('location').pathname = PAGE_URL;
    resolveSskuRequests(app.api);

    const supplier = testSupplier();
    const offer = testDisplayOffer({ shopSkuKey: { supplierId: supplier.id, shopSku: 'shopSku' } });

    resolveResultSskuRequest(app.api, testResultAvailability({ offer }));
    resolveSuppliers(app.api, [supplier]);
    resolvePartnerRelation(app.api);
    resolveSeasonalMskuIdsList(app.api);
    resolveAssortSskuRequest(app.api);
    resolveSeasonalDictionary(app.api);

    // Check all requests are resolved, i.e. no unknown requests
    expect(app.api.allActiveRequests).toEqual({});

    const table = app.find(ResultAvailabilityTable);
    const filter = app.find(ResultAvailabilityFilterForm);
    expect(table).toHaveLength(1);
    expect(filter).toHaveLength(1);
    app.unmount();
  });

  it('renders one ssku row', () => {
    const app = new TestCmApp(`${PAGE_URL}?categoryManagerLogin=manager&hidingSubReasons=45k`);
    resolveSskuRequests(app.api);

    const msku = testDisplayMsku({ title: 'Test_Msku' });
    const offer = testDisplayOffer({ title: 'Test_Offer' });

    const blockingText = 'Запрещены поставки msku Steel Power Tools';
    const warehouse = twoTestWarehouses[0];
    const availabilitiesByWarehouseId = {
      [warehouse.id]: [
        {
          reason: Reason.MSKU,
          shortText: blockingText,
          fullText: blockingText,
          params: {},
          available: false,
        },
      ],
    };

    const ssku = testResultAvailability({ msku, offer, availabilitiesByWarehouseId });
    resolveResultSskuRequest(app.api, ssku);

    const shopSkuKey = getShopSkuKeysSelector(app.store.getState());
    expect(shopSkuKey).toHaveLength(1);

    const tBody = app.app.find('tbody');
    const tRows = tBody.find(SupplierRow);
    expect(tRows).toHaveLength(1);

    const offerRow = tRows.last();
    const offerRowHtml = offerRow.html();

    expect(offerRowHtml).toContain(msku.title);
    expect(offerRowHtml).toContain(offer.title);
    expect(offerRowHtml).toContain(blockingText);

    app.unmount();
  });

  it('renders warehauses columns', () => {
    const app = new TestCmApp(`${PAGE_URL}?categoryManagerLogin=manager&hidingReasonKeys=45k`);
    resolveSskuRequests(app.api);

    const warehous = twoTestWarehouses[0];

    const tableHead = app.app.find('thead');
    expect(tableHead.html()).toMatch(warehous.name);

    app.unmount();
  });

  it('renders group by supplier', () => {
    const app = new TestCmApp(`${PAGE_URL}?categoryManagerLogin=manager&hidingReasonKeys=45k`);
    resolveSskuRequests(app.api);

    const supplier = testSupplier({ realSupplierId: 'RSID=00093' });
    const offer = testDisplayOffer({ shopSkuKey: { supplierId: supplier.id, shopSku: 'shopSku' } });

    resolveResultSskuRequest(app.api, testResultAvailability({ offer }));
    resolveSuppliers(app.api, [supplier]);

    const tableBody = app.app.find('tbody');
    expect(tableBody).toHaveLength(1);
    const tableRow = tableBody.find('tr');
    // first row for group by supplier, second row for display offer
    expect(tableRow).toHaveLength(2);
    const supplierRow = tableRow.first();

    const tableRoHtml = supplierRow.html();
    expect(tableRoHtml).toMatch(supplier.name);
    expect(tableRoHtml).toMatch(supplier.realSupplierId!);

    const supplierLink = supplierRow.find(Link);
    expect(supplierLink).toHaveLength(1);
    expect(supplierLink.html()).toMatch(supplier.id.toString());

    app.unmount();
  });
});
