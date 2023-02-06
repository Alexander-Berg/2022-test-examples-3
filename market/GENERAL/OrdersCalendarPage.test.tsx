import { ReactWrapper } from 'enzyme';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { OrdersCalendarPage } from '.';
import { setupTestApp } from 'src/test/setupApp';
import Api from 'src/Api';

let app: ReactWrapper;
let api: MockedApiObject<Api>;

describe('OrdersCalendarPage', () => {
  beforeEach(() => {
    ({ app, api } = setupTestApp('/orders-calendar/1?year=2022'));

    api.calendarWorkController.getCalendars.next().resolve([{ id: 1, name: 'test' }]);
    app.update();
  });

  const getOrdersCalendarPage = () => app.find(OrdersCalendarPage);

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('<OrdersCalendarPage />', () => {
    it('should be render without errors', () => {
      expect(getOrdersCalendarPage()).toHaveLength(1);
      expect(api.calendarWorkController.getCalendar).toBeCalledWith(2022, undefined, 1);
      expect(app.find('h1').text()).toBe('Календарь заказов: test');
    });
  });
});
