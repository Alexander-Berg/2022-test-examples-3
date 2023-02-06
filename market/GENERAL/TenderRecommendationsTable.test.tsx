import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { ReactWrapper } from 'enzyme';
import { of } from 'ramda';

import { ReplenishmentDataGrid } from 'src/pages/replenishment/components';
import { DemandType } from 'src/java/definitions-replenishment';
import { recommendationsCount } from 'src/test/data/recomendationsCount';
import { createRecommendation } from 'src/test/data/recomendations';
import { suppliers } from 'src/test/data/replenishmentSuppliers';
import { TenderTable } from './components';
import { createDemandDTO } from 'src/test/data/demand';
import { setupTestApp } from 'src/test/setupApp';
import { Loader } from 'src/components';
import Api from 'src/Api';

let app: ReactWrapper;
let api: MockedApiObject<Api>;

const DEMAND_ID = 1337;
const DEMAND_MOCK = createDemandDTO({ id: DEMAND_ID, supplier: suppliers[1], demandType: DemandType.TENDER });

const RECOMMENDATIONS_MOCK = [
  createRecommendation({ msku: 1, setQuantity: 1, correctionReason: { name: '', id: 0 } }),
  createRecommendation({ msku: 2, setQuantity: 1, correctionReason: { name: '', id: 0 } }),
  createRecommendation({ msku: 3, setQuantity: 1, correctionReason: { name: '', id: 0 } }),
];

describe('TenderDetailPage <TenderRecommendationsTable />', () => {
  beforeEach(() => {
    ({ app, api } = setupTestApp(`/replenishment/tender/${DEMAND_ID}`, false));

    api.demandController.demands.next().resolve(of(DEMAND_MOCK));

    api.recommendationController.tenderReplenishmentWithCountPost.next().resolve({
      recommendations: RECOMMENDATIONS_MOCK,
      numberOfCorrectedRecommendations: 0,
      userFiltersCount: [],
      count: recommendationsCount,
    });

    app.update();
  });

  const getTenderTable = () => app.find(TenderTable);
  it('Should mount correctly', () => {
    const table = getTenderTable();

    expect(table).toHaveLength(1);
    expect(api.correctionReasonController.getCorrectionReasons).toHaveBeenCalled();
    expect(api.replenishmentWarehouseController.getWarehouses).toHaveBeenCalled();
    expect(api.demandController.demands).toHaveBeenCalled();

    app.update();
    expect(table.find(ReplenishmentDataGrid)).toHaveLength(1);
    expect(table.find(Loader)).toHaveLength(0);
    expect(table.find(ReplenishmentDataGrid)).toHaveLength(1);
  });
});
