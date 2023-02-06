import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { act } from 'react-dom/test-utils';
import { ReactWrapper } from 'enzyme';
import MockDate from 'mockdate';

import Api from 'src/Api';
import { ResponsibleFilter } from 'src/pages/replenishment/components';
import { warehouses } from 'src/test/data/replenishmentWarehouses';
import { DemandStatus } from 'src/java/definitions-replenishment';
import { ReplenishmentCalendar } from './ReplenishmentCalendar';
import { Select, SelectControl, Button } from 'src/components';
import { ReplenishmentPage3P } from './ReplenishmentPage3P';
import { users } from 'src/test/data/replenishmentUsers';
import { waitForPromises } from 'src/test/utils/utils';
import { setupTestApp } from 'src/test/setupApp';

const MOCKED_CURRENT_DATE = new Date('2020-05-17');
const MOCK_DATE = '2020-05-24';

let app: ReactWrapper;
let api: MockedApiObject<Api>;

beforeAll(() => {
  MockDate.set(MOCKED_CURRENT_DATE);
});

beforeEach(() => {
  ({ app, api } = setupTestApp(`/replenishment/3p`));
  api.replenishmentWarehouseController.getWarehouses.next().resolve(warehouses);
  app.update();
});

afterAll(() => {
  MockDate.reset();
});

describe('<ReplenishmentPage /> (3P)', () => {
  const getReplenishmentPage = () => app.find(ReplenishmentPage3P);

  it('Should mount correctly', () => {
    expect(getReplenishmentPage()).toHaveLength(1);
    expect(api.replenishmentWarehouseController.getWarehouses).toBeCalledTimes(1);
    expect(api.replenishmentUserController.getAll).toBeCalledTimes(1);
  });

  it('Should set filters correctly', async () => {
    await act(async () => {
      // set region
      getReplenishmentPage().find(SelectControl).filter('[label="Регион"]').prop('onChange')({
        label: warehouses[1].region.name,
        value: warehouses[1].region.id,
      });
      await waitForPromises();
      app.update();

      // set status
      getReplenishmentPage().find(SelectControl).filter('[label="Статус"]').prop('onChange')({
        label: DemandStatus.REVIEWED,
        value: DemandStatus.REVIEWED,
      });
      await waitForPromises();
      app.update();

      // set catman
      getReplenishmentPage().find(ResponsibleFilter).find(Select).prop('onChange')({
        label: users[1].login,
        value: users[1].login,
      });
      await waitForPromises();
      app.update();

      // set date
      getReplenishmentPage().find('.control').find(Button).at(2).simulate('click');
      app.update();
    });

    await waitForPromises();
    app.update();

    expect(getReplenishmentPage().find(ReplenishmentCalendar).prop('queryParams')).toEqual({
      regionId: `${warehouses[1].region.id}`,
      status: DemandStatus.REVIEWED,
      responsible: users[1].login,
      dateFrom: MOCK_DATE,
    });
  });
});
