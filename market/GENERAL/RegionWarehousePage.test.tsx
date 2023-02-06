import { ReactWrapper } from 'enzyme';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import Api from 'src/Api';
import { setupTestApp } from 'src/test/setupApp';
import { warehouses } from 'src/test/data/replenishmentWarehouses';
import { RegionWarehousePage } from './RegionWarehousePage';

let app: ReactWrapper;
let api: MockedApiObject<Api>;

beforeEach(() => {
  ({ app, api } = setupTestApp(`/region-warehouse`));
  api.replenishmentWarehouseController.getWarehouses.next().resolve(warehouses);
  app.update();
});

describe('<RegionWarehousePage />', () => {
  const getPage = () => app.find(RegionWarehousePage);

  it('Should mount correctly', () => {
    expect(getPage()).toHaveLength(1);
  });
});
