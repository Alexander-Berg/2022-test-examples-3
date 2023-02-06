import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { ReactWrapper } from 'enzyme';

import Api from 'src/Api';
import { movements } from 'src/test/data/interWarehouseMovements';
import { warehouses } from 'src/test/data/replenishmentWarehouses';
import { setupTestApp } from 'src/test/setupApp';
import { InterWarehouseMovementPage } from './InterWarehouseMovementPage';
import { MovementsFilter, MovementsTable } from './components';

let app: ReactWrapper;
let api: MockedApiObject<Api>;

beforeEach(() => {
  ({ app, api } = setupTestApp('/replenishment/movements/inter-warehouse'));
  api.replenishmentWarehouseController.getWarehouses.next().resolve(warehouses);
  api.interWarehouseMovementsController.stats.next().resolve({ count: 2 });
  api.interWarehouseMovementsController.recommendations.next().resolve(movements);

  app.update();
});

describe('<InterWarehouseMovementsPage />', () => {
  const getMovementsPage = () => app.find(InterWarehouseMovementPage);

  it('Should mount correctly', () => {
    expect(getMovementsPage()).toHaveLength(1);
    expect(app.find(MovementsTable)).toHaveLength(1);
    expect(app.find(MovementsFilter)).toHaveLength(1);

    expect(api.replenishmentWarehouseController.getWarehouses).toBeCalledTimes(1);
    expect(api.interWarehouseMovementsController.stats).toBeCalledTimes(1);
    expect(api.interWarehouseMovementsController.recommendations).toBeCalledTimes(1);
  });
});
