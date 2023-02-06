import { ReactWrapper } from 'enzyme';
import { act } from 'react-dom/test-utils';
import { Store } from 'redux';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { setupTestApp } from 'src/test/setupApp';
import Api from 'src/Api';
import { warehouses } from 'src/test/data/replenishmentWarehouses';
import { XdocScheduleDTO } from 'src/java/definitions-replenishment';
import { XDockSchedulePage } from './XDockSchedulePage';
import { XDockWarehouse, XDockWarehouseLine } from './components';
import { Button, TextInput } from 'src/components';

let app: ReactWrapper;
let api: MockedApiObject<Api>;
let store: Store;

const schedule: XdocScheduleDTO[] = [
  {
    xdocId: 102,
    warehouseId: 103,
    schedule: [5, 4, 4, 3, 8, 7, 6],
  },
  {
    xdocId: 102,
    warehouseId: 104,
    schedule: [5, 5, 4, 9, 8, null, null],
  },
  {
    xdocId: 102,
    warehouseId: 108,
    schedule: [5, 4, 4, 3, 8, 7, 6],
  },
];

describe('XDockSchedulePage', () => {
  beforeEach(() => {
    ({ app, api, store } = setupTestApp(`/xdock-schedule`, false));
    api.replenishmentWarehouseController.getWarehouses.next().resolve(warehouses);
    api.xdocScheduleController.save.mockResolvedValue();
    api.xdocScheduleController.delete.mockResolvedValue();
  });

  afterEach(() => {
    app.unmount();
  });

  it('renders page correctly', async () => {
    api.xdocScheduleController.getAll.next().resolve(schedule);
    app.update();

    expect(app.find(XDockSchedulePage)).toHaveLength(1);
    expect(store.getState().replenishment.xdocSchedules).toEqual(schedule);
    expect(app.find(XDockWarehouse)).toHaveLength(1);
    expect(app.find(XDockWarehouseLine)).toHaveLength(3);

    // кликаем добавить
    app.find(XDockWarehouse).at(0).find(Button).at(0).simulate('click');
    expect(app.find(XDockWarehouseLine)).toHaveLength(4);
    // кликаем удалить на новой строке
    app.find(XDockWarehouseLine).at(0).find(Button).at(2).simulate('click');
    expect(app.find(XDockWarehouseLine)).toHaveLength(3);
    // кликаем удалить на первой строке
    await act(() => {
      app.find(XDockWarehouseLine).at(0).find(Button).at(2).simulate('click');
    });
    app.update();
    expect(api.xdocScheduleController.delete).toBeCalledWith({
      warehouseId: schedule[0].warehouseId,
      xdocId: schedule[0].xdocId,
    });
    expect(store.getState().replenishment.xdocSchedules).toEqual(schedule.slice(1));
    expect(app.find(XDockWarehouseLine)).toHaveLength(2);
    // меняем значение на понедельник
    app
      .find(XDockWarehouseLine)
      .at(0)
      .find(TextInput)
      .at(0)
      .find('input')
      .at(0)
      .simulate('change', { target: { value: 15 } });
    app.update();
    await act(() => {
      // кликаем сохранить
      app.find(XDockWarehouseLine).at(0).find(Button).at(0).simulate('click');
    });
    app.update();
    expect(api.xdocScheduleController.save).toBeCalledWith([
      {
        warehouseId: schedule[1].warehouseId,
        xdocId: schedule[1].xdocId,
        schedule: [15, 5, 4, 9, 8, null, null],
      },
    ]);
    expect(store.getState().replenishment.xdocSchedules[0]).toEqual({
      warehouseId: schedule[1].warehouseId,
      xdocId: schedule[1].xdocId,
      schedule: [15, 5, 4, 9, 8, null, null],
    });
  });

  it('should render multiple xdocs', () => {
    schedule[0].xdocId = 108;
    schedule[1].xdocId = 103;
    api.xdocScheduleController.getAll.next().resolve(schedule);
    app.update();
    expect(app.find(XDockWarehouse)).toHaveLength(3);
  });
});
