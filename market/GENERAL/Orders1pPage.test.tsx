import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { ReactWrapper } from 'enzyme';
import MockDate from 'mockdate';

import Api from 'src/Api';
import { warehouses } from 'src/test/data/replenishmentWarehouses';
import { suppliers } from 'src/test/data/replenishmentSuppliers';
import { users } from 'src/test/data/replenishmentUsers';
import { Orders1pPage } from './Orders1pPage';
import { setupTestApp } from 'src/test/setupApp';
import { Orders1pTable } from './components';

const MOCKED_CURRENT_DATE = new Date('2020-05-17');
const MOCK_DATE = '2020-05-24';

let app: ReactWrapper;
let api: MockedApiObject<Api>;

beforeAll(() => {
  MockDate.set(MOCKED_CURRENT_DATE);
});

beforeEach(() => {
  ({ app, api } = setupTestApp(`/replenishment/orders`));
  api.replenishmentWarehouseController.getWarehouses.next().resolve(warehouses);
  app.update();
});

afterAll(() => {
  MockDate.reset();
});

describe('<Orders1pPage />', () => {
  const getPage = () => app.find(Orders1pPage);

  it('Should mount correctly', () => {
    expect(getPage()).toHaveLength(1);
    expect(api.recommendationsImportController.getRecommendationsImportStatus).toBeCalledTimes(1);
    expect(api.replenishmentWarehouseController.getWarehouses).toBeCalledTimes(1);
    expect(api.replenishmentUserController.getAll).toBeCalledTimes(1);
  });

  it('Should parse params from location', () => {
    ({ app, api } = setupTestApp(
      `/replenishment/orders?dateFrom=${MOCK_DATE}&responsible=${users[1].login}&supplierId=${suppliers[1].id}&supplyRoute=xdoc&warehouseId=${warehouses[1].id}`
    ));
    app.update();

    const table = app.find(Orders1pTable);
    expect(table).toHaveLength(1);

    const actual = table.prop('filter');
    expect(actual).toMatchObject({
      responsible: users[1].login,
      supplierId: suppliers[1].id,
      supplyRoute: 'xdoc',
      warehouseId: warehouses[1].id,
    });
  });
});
