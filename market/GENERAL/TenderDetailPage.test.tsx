import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { ReactWrapper } from 'enzyme';
import { of } from 'ramda';

import { TenderResultsGrid, TenderSummaryTable, TenderSupplierResponseSelector } from './components';
import { createTenderSupplierResponse } from 'src/test/data/tenderSupplierResponse';
import { DemandType, TenderStatus } from 'src/java/definitions-replenishment';
import { recommendationsCount } from 'src/test/data/recomendationsCount';
import { createRecommendation } from 'src/test/data/recomendations';
import { correctionReasons } from 'src/test/data/correctionReasons';
import { suppliers } from 'src/test/data/replenishmentSuppliers';
import { createDemandDTO } from 'src/test/data/demand';
import { TenderDetailPage } from './TenderDetailPage';
import { setupTestApp } from 'src/test/setupApp';
import { Attach, Button } from 'src/components';
import Api from 'src/Api';
import { BulkEdit } from 'src/pages/replenishment/ReplenishmentDetailPage/components';

let app: ReactWrapper;
let api: MockedApiObject<Api>;

const DEMAND_ID = 1337;
const DEMAND_VERSION = 1;

const DEMAND_MOCK = createDemandDTO({
  id: DEMAND_ID,
  version: DEMAND_VERSION,
  supplier: suppliers[1],
  orderId: 'testOrderId',
  demandType: DemandType.TENDER,
  tenderStatus: TenderStatus.STARTED,
});

const RECOMMENDATIONS_MOCK = [
  createRecommendation({ msku: 1, setQuantity: 1, vendorId: 111, correctionReason: { name: '', id: 0 } }),
  createRecommendation({ msku: 2, setQuantity: 1, vendorId: 111, correctionReason: { name: '', id: 0 } }),
  createRecommendation({ msku: 3, setQuantity: 1, vendorId: 222, correctionReason: { name: '', id: 0 } }),
];

const TENDER_RESPONSE_MOCKS = suppliers.map(({ id: supplierId, name: supplierName }) =>
  createTenderSupplierResponse({ supplierId, supplierName })
);

function baseInit(tenderStatus: TenderStatus) {
  ({ app, api } = setupTestApp(`/replenishment/tender/${DEMAND_ID}`));
  api.demandController.demands.next().resolve(of({ ...DEMAND_MOCK, tenderStatus }));
}

function initWithRecommendations(tenderStatus: TenderStatus) {
  baseInit(tenderStatus);

  api.correctionReasonController.getCorrectionReasons.next().resolve(correctionReasons);
  api.recommendationController.tenderReplenishmentWithCountPost.next().resolve({
    recommendations: RECOMMENDATIONS_MOCK,
    numberOfCorrectedRecommendations: 0,
    userFiltersCount: [],
    count: recommendationsCount,
  });

  app.update();
}

function initWithResponses(tenderStatus: TenderStatus) {
  baseInit(tenderStatus);
  api.tenderController.getSupplierResponses.next().resolve({ items: TENDER_RESPONSE_MOCKS });
  app.update();
}

describe('<TenderDetailPage />', () => {
  beforeEach(() => initWithRecommendations(TenderStatus.STARTED));

  const getPage = () => app.find(TenderDetailPage);

  it('should mount correctly', () => {
    expect(getPage()).toHaveLength(1);
    expect(api.demandController.demands).toBeCalledWith({
      id: of(DEMAND_ID),
      demandType: DemandType.TENDER,
    });
  });

  it('Верные элементы для TenderStatus.NEW', () => {
    initWithRecommendations(TenderStatus.NEW);

    const page = getPage();
    expect(page).toHaveLength(1);

    expect(page.find(BulkEdit)).toHaveLength(1);
    expect(page.find(TenderSummaryTable)).toHaveLength(0);
    expect(page.find(TenderSupplierResponseSelector)).toHaveLength(0);
    expect(page.find('#download-template').find(Button)).toHaveLength(0);
    expect(page.find(TenderResultsGrid)).toHaveLength(0);
  });

  it('Верные элементы для TenderStatus.STARTED', () => {
    const page = getPage();
    expect(page).toHaveLength(1);

    expect(page.find(BulkEdit)).toHaveLength(0);
    expect(page.find(TenderSummaryTable)).toHaveLength(1);
    expect(page.find(TenderSupplierResponseSelector)).toHaveLength(1);
    expect(page.find('#download-template').find(Button)).toHaveLength(1);
    expect(page.find(TenderResultsGrid)).toHaveLength(0);
  });

  it('Кнопка загрузки ответа недоступна до выбора поставщика', () => {
    const attach = getPage().find(TenderSupplierResponseSelector).find(Attach);
    expect(attach.at(0).props().disabled).toBe(true);
  });

  it('Should render correctly for TenderStatus.OFFERS_COLLECTED', () => {
    initWithResponses(TenderStatus.OFFERS_COLLECTED);

    const page = getPage();
    expect(page).toHaveLength(1);

    expect(page.find(BulkEdit)).toHaveLength(0);
    expect(page.find(TenderSummaryTable)).toHaveLength(1);
    expect(page.find(TenderSupplierResponseSelector)).toHaveLength(0);
    expect(page.find('#download-template').find(Button)).toHaveLength(0);
    expect(page.find(TenderResultsGrid)).toHaveLength(1);
  });
});
