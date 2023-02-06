import { act } from 'react-dom/test-utils';
import { ReactWrapper } from 'enzyme';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { TrucksDivide } from './TrucksDivide';
import { setupTestApp } from 'src/test/setupApp';
import Api from 'src/Api';
import { createDemandDTO } from 'src/test/data/demand';
import { suppliers } from 'src/test/data/replenishmentSuppliers';
import { Alert, Button } from 'src/components';
import { TruckCard } from 'src/pages/replenishment/ReplenishmentDetailPage/components/DemandInfo/components/DirectDemandActions/components/TrucksDivide/TruckCard';

let app: ReactWrapper;
let api: MockedApiObject<Api>;

const DEMAND_ID = 1337;
const DEMAND_MOCK = createDemandDTO({ id: DEMAND_ID, supplier: suppliers[1] });
const TRUKCS_MOCK = [
  { pallets: 33, weight: 1719, fullness: 1, items: 1 },
  { pallets: 26, weight: 1431, fullness: 0.78, items: 2 },
];
const TRUKCS_INFO_MOCK = {
  minPallets: 29,
  maxPallets: 33,
  palletMaxHeight: 180,
  palletMinWeight: 0,
  palletMaxWeight: 550,
  truckMinWeight: 0,
  truckMaxWeight: 18500,
  trucks: [],
  error: null,
};

beforeEach(() => {
  ({ app, api } = setupTestApp(`/replenishment/${DEMAND_ID}`));

  api.demandController.demands.next().resolve([DEMAND_MOCK]);

  act(() => api.supplierTruckParamsController.hasSupplierTruckParams.next().resolve(true));

  app.update();
});

describe('<TrucksDivide/>', () => {
  const getTrucksDivide = () => app.find(TrucksDivide);

  it('Should mount correctly', () => {
    act(() => api.demandController.getTruckSplitInfo.next().resolve({ ...TRUKCS_INFO_MOCK, trucks: TRUKCS_MOCK }));
    app.update();

    const btn = getTrucksDivide().find(Button);
    expect(btn.text().trim()).toEqual('89%');
    btn.simulate('click');
    expect(getTrucksDivide().find(TruckCard).at(0).text()).toContain('78%');
  });

  it('Should show error message', () => {
    act(() => api.demandController.getTruckSplitInfo.next().resolve({ ...TRUKCS_INFO_MOCK, error: 'test' }));
    app.update();

    getTrucksDivide().find(Button).simulate('click');
    const trucksDivide = getTrucksDivide();
    expect(trucksDivide.find(Alert).text()).toEqual('test');
  });
});
