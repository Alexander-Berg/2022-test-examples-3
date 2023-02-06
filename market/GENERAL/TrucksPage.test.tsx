import { DatePicker } from '@yandex-market/mbo-components';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { ReactWrapper } from 'enzyme';

import Api from 'src/Api';
import { Button } from 'src/components';
import { TruckDTO } from 'src/java/definitions-replenishment';
import TrucksItem from 'src/pages/replenishment/TrucksPageNew/TrucksItem/TrucksItem';
import { TrucksPage } from 'src/pages/replenishment/TrucksPageNew/TrucksPage';
import { setupTestApp } from 'src/test/setupApp';

const DATE_FROM_STR = '2019-01-01';
const DATE_TO_STR = '2019-02-02';

const trucks: TruckDTO[] = [
  {
    code: 101,
    id: 1,
    status: { id: 1, name: 'Новый' },
    warehouseFrom: { id: 145, name: 'Маршрут', code: 200 },
    warehouseTo: { id: 147, name: 'Ростов', code: 100 },
  },
  {
    code: 102,
    id: 2,
    status: { id: 1, name: 'Новый' },
    warehouseFrom: { id: 145, name: 'Маршрут', code: 200 },
    warehouseTo: { id: 147, name: 'Ростов', code: 100 },
  },
  {
    axOrderId: 'b521337',
    code: 103,
    id: 3,
    status: { id: 3, name: 'Создан заказ' },
    warehouseFrom: { id: 147, name: 'Ростов', code: 100 },
    warehouseTo: { id: 145, name: 'Маршрут', code: 200 },
  },
] as TruckDTO[];

let app: ReactWrapper;
let api: MockedApiObject<Api>;

beforeEach(() => {
  ({ app, api } = setupTestApp(`/replenishment/inter-warehouse/trucks`));
});

describe('<TrucksPageNew />', () => {
  const setTrucks = () => api.truckController.trucks.next().resolve(trucks);

  const getTrucksPage = () => app.find(TrucksPage).first();
  const getFirstTruckItem = () => app.find(TrucksItem).first();
  const getLastTruckItem = () => app.find(TrucksItem).last();
  const getSelectedTrucks = () => getTrucksPage().state('selectedTrucks');
  const getDatePickerFrom = () => getTrucksPage().find(DatePicker).first();
  const getDatePickerTo = () => getTrucksPage().find(DatePicker).last();

  it('Should load trucks', () => {
    expect(api.truckController.trucks).toBeCalledTimes(1);
  });

  it('Should render trucksItems', () => {
    setTrucks();
    app.update();

    expect(app.find(TrucksItem)).toHaveLength(3);
  });

  it('Should select & unselect trucksItem', () => {
    setTrucks();
    app.update();

    const firstTruckItem = getFirstTruckItem();
    const lastTruckItem = getLastTruckItem();

    firstTruckItem.simulate('click');
    expect(getSelectedTrucks().size).toBe(1);

    firstTruckItem.simulate('click');
    expect(getSelectedTrucks().size).toBe(0);

    // Not allow select not-new truck
    lastTruckItem.simulate('click');
    expect(getSelectedTrucks().size).toBe(0);
  });

  it('Should export selected trucks', () => {
    setTrucks();
    app.update();

    const firstTruckItem = getFirstTruckItem();
    const trucksPage = getTrucksPage();
    const exportButton = trucksPage.find(Button).last();
    firstTruckItem.simulate('click');
    exportButton.simulate('click');

    expect(api.truckController.saveForExport).toBeCalledTimes(1);
    expect(getSelectedTrucks().size).toBe(0);
  });

  it('Should select date correctly', () => {
    const trucksPage = getTrucksPage();
    const datePickers = trucksPage.find(DatePicker);
    const datePickerFrom = getDatePickerFrom();
    const datePickerTo = getDatePickerTo();
    expect(datePickers).toHaveLength(2);
    datePickerTo.find('input').simulate('change', { target: { value: DATE_TO_STR } });
    datePickerFrom.find('input').simulate('change', { target: { value: DATE_FROM_STR } });
    trucksPage.update();
    expect(trucksPage.state('dateFrom').toLocaleDateString()).toEqual(new Date(DATE_FROM_STR).toLocaleDateString());
    expect(trucksPage.state('dateTo').toLocaleDateString()).toEqual(new Date(DATE_TO_STR).toLocaleDateString());
  });
});
