import { DataTableRow } from '@yandex-market/mbo-components/es/components/DataTable/Row/DataTableRow';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { DataTable } from '@yandex-market/mbo-components';
import { ReactWrapper } from 'enzyme';
import { of } from 'ramda';
import { act } from 'react-dom/test-utils';

import { recommendationsCount } from 'src/test/data/recomendationsCount';
import { createDemandDTO } from 'src/test/data/demand';
import { createRecommendation } from 'src/test/data/recomendations';
import { suppliers } from 'src/test/data/replenishmentSuppliers';
import { setupTestApp } from 'src/test/setupApp';
import { DemandsUnite } from './DemandsUnite';
import { Button } from 'src/components';
import Api from 'src/Api';
import { DemandType, DemandUnionRequest } from 'src/java/definitions-replenishment';

let app: ReactWrapper;
let api: MockedApiObject<Api>;

const DEMAND_ID = 1337;
const DEMAND_IDENTITY = { id: 1337, version: 1 };
const DEMAND_MOCK = createDemandDTO({ id: DEMAND_ID, supplier: suppliers[1] });
const RECOMMENDATIONS_MOCK = [
  createRecommendation({ msku: 1, setQuantity: 1, vendorId: 111, correctionReason: { name: '', id: 0 } }),
  createRecommendation({ msku: 2, setQuantity: 1, vendorId: 111, correctionReason: { name: '', id: 0 } }),
  createRecommendation({ msku: 3, setQuantity: 1, vendorId: 222, correctionReason: { name: '', id: 0 } }),
];
const DEMANDS = [createDemandDTO({ id: 1111 }), createDemandDTO({ id: 2222 }), createDemandDTO({ id: 3333 })];

beforeEach(() => {
  ({ app, api } = setupTestApp(`/replenishment/${DEMAND_ID}`));

  api.demandController.demands.next().resolve(of(DEMAND_MOCK));
  api.recommendationController.replenishmentWithCountPost.next().resolve({
    count: recommendationsCount,
    userFiltersCount: [],
    numberOfCorrectedRecommendations: 0,
    recommendations: RECOMMENDATIONS_MOCK,
  });

  act(() => api.demandController.demandsToUnite.next().resolve(DEMANDS));
  act(() => api.demandController.getUnionDemands.next().resolve(DEMANDS));

  app.update();
});

describe('<ReplenishmentUnite />', () => {
  const getReplenishmentUnite = () => app.find(DemandsUnite);

  const getDataTableRows = () => getReplenishmentUnite().find(DataTable).find(DataTableRow);

  it('Should select & unite demands', () => {
    // since lego doesn't render popups until they were called
    // we need to simulate its opening
    getReplenishmentUnite()
      .findWhere(node => node.type() === Button && node.text() === 'Объединить')
      .simulate('click');

    getDataTableRows().at(1).simulate('mouseDown');
    getDataTableRows().at(2).simulate('mouseDown');
    const submitButton = getReplenishmentUnite().find(Button).last();
    expect(submitButton.text()).toBe('Применить');
    submitButton.simulate('click');
    expect(api.demandController.uniteDemands).toBeCalledWith(
      {
        demandKeys: [
          { id: 1337, version: 1 },
          { id: 2222, version: 1 },
          { id: 3333, version: 1 },
        ],
        demandsToUnite: [2222, 3333],
        mainDemandId: 1337,
      } as DemandUnionRequest,
      DemandType.TYPE_1P
    );
  });

  it('Should undo union', () => {
    getReplenishmentUnite()
      .findWhere(node => node.type() === Button && node.text() === 'Отменить объединение')
      .simulate('click');
    const submitButton = getReplenishmentUnite().find(Button).last();
    expect(submitButton.text()).toBe('Применить');
    submitButton.simulate('click');
    expect(api.demandController.undoUnionDemands).toHaveBeenCalledWith(DEMAND_IDENTITY, DemandType.TYPE_1P);
  });
});
