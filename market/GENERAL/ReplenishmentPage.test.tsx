import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { act } from 'react-dom/test-utils';
import { ReactWrapper } from 'enzyme';
import MockDate from 'mockdate';
import { Store } from 'redux';
import { of } from 'ramda';

import Api from 'src/Api';
import { ReplenishmentCalendarItem } from './ReplenishmentCalendar/ReplenishmentCalendarItem';
import { CatteamsFilter, ResponsibleFilter, SupplierFilter, SupplyTypeSelect } from '../components';
import { DateType, DemandStatus } from 'src/java/definitions-replenishment';
import { Button, Select, SelectControl } from 'src/components';
import { warehouses } from 'src/test/data/replenishmentWarehouses';
import { suppliers } from 'src/test/data/replenishmentSuppliers';
import { ReplenishmentCalendar } from './ReplenishmentCalendar';
import { users } from 'src/test/data/replenishmentUsers';
import { ReplenishmentPage } from './ReplenishmentPage';
import { createDemandDTO } from 'src/test/data/demand';
import { catTeams } from 'src/test/data/catTeams';
import { setupTestApp } from 'src/test/setupApp';
import { setCurrentUser } from 'src/store/root/current-user/current-user.actions';

const MOCK_DATE = '2020-05-24';
const MOCKED_TODAY = '2020-05-17';
const MOCKED_CURRENT_DATE = new Date(MOCKED_TODAY);

const testUser = {
  id: 1,
  login: 'login',
  roles: [],
  firstName: 'login',
  lastName: 'login',
  staffUpdateTs: new Date().toISOString(),
};

let app: ReactWrapper;
let api: MockedApiObject<Api>;
let store: Store;

beforeAll(() => {
  MockDate.set(MOCKED_CURRENT_DATE);
});

beforeEach(() => {
  ({ app, api, store } = setupTestApp(`/replenishment`));
  store.dispatch(setCurrentUser(testUser));
  api.replenishmentWarehouseController.getWarehouses.next().resolve(warehouses);
  api.replenishmentUserController.getAll.next().resolve(users);
  api.catteamsController.getCatteams.next().resolve([]);
  app.update();
});

afterAll(() => {
  MockDate.reset();
});

describe('<ReplenishmentPage />', () => {
  const getReplenishmentPage = () => app.find(ReplenishmentPage);

  it('Should mount correctly', () => {
    expect(getReplenishmentPage()).toHaveLength(1);
    expect(api.recommendationsImportController.getRecommendationsImportStatus).toBeCalledTimes(1);
    expect(api.replenishmentWarehouseController.getWarehouses).toBeCalledTimes(1);
    expect(api.replenishmentUserController.getAll).toBeCalledTimes(1);
  });

  it('Should set filters correctly', async () => {
    await act(async () => {
      // set dateType
      await getReplenishmentPage().find(SelectControl).filter('[label="Отображать по"]').prop('onChange')({
        label: 'Дате поставки',
        value: DateType.DELIVERY,
      });
      app.update();

      // set warehouse
      getReplenishmentPage().find(SelectControl).filter('[label="Склад"]').prop('onChange')({
        label: warehouses[1].name,
        value: warehouses[1].id,
      });
      app.update();

      // set type
      getReplenishmentPage().find(SupplyTypeSelect).prop('onChange')({
        label: 'X-Dock',
        value: 'xdoc',
      });
      app.update();

      // set status
      getReplenishmentPage().find(SelectControl).filter('[label="Статус"]').prop('onChange')({
        label: DemandStatus.REVIEWED,
        value: DemandStatus.REVIEWED,
      });
      app.update();

      // set supplier
      getReplenishmentPage().find(SupplierFilter).find(Select).prop('onChange')({
        label: suppliers[1].name,
        value: `${suppliers[1].id}`,
      });
      app.update();

      // set catteam
      getReplenishmentPage().find(CatteamsFilter).find(Select).prop('onChange')({
        label: catTeams[0].catteam,
        value: catTeams[0].id,
      });
      app.update();

      // set catman
      getReplenishmentPage().find(ResponsibleFilter).find(Select).prop('onChange')({
        label: users[1].login,
        value: users[1].login,
      });
      app.update();

      // set date
      getReplenishmentPage().find('.control').find(Button).at(2).simulate('click');
      app.update();
    });

    expect(store.getState().replenishment.savedReplenishmentFilter).toMatchObject({
      catteamId: 1,
      responsible: users[1].login,
      dateFrom: MOCK_DATE,
      dateType: DateType.DELIVERY,
      supplierId: suppliers[1].id,
      status: DemandStatus.REVIEWED,
      supplyRoute: 'xdoc',
      warehouseId: warehouses[1].id,
    });
  });

  it('should render autoProcessing icon for autoprocessed DemandDTO', () => {
    api.demandController.demands.next().resolve(
      of(
        createDemandDTO({
          autoProcessing: true,
          orderDate: MOCKED_TODAY,
          deliveryDate: MOCKED_TODAY,
          // @NOTE: @nobody - это плейсхолдер на случай, когда приложенька не может получить юзера
          catman: '@nobody',
        })
      )
    );

    app.update();
    expect(api.demandController.demands).toBeCalled();

    const calendars = app.find(ReplenishmentCalendar);
    expect(calendars).toHaveLength(1);

    const items = calendars.first().find(ReplenishmentCalendarItem);
    expect(items).toHaveLength(1);

    expect(items.first().find('[title="Автоматический заказ"]')).toHaveLength(1);
  });
});
