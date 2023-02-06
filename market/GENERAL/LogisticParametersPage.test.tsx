import { act } from 'react-dom/test-utils';
import { ReactWrapper } from 'enzyme';
import { Store } from 'redux';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { LogisticsParamDTO, Warehouse, WarehouseType } from 'src/java/definitions-replenishment';
import { LogisticTableRowItem } from './components/LogisticTable/LogisticTableRowItem';
import { LogisticParametersPage } from 'src/pages/logistics/LogisticParametersPage';
import { TimeSlot, WeekDay } from './components/TimeSlotModal/components';
import replenishmentActions from 'src/pages/replenishment/store/actions';
import { warehouses } from 'src/test/data/replenishmentWarehouses';
import { suppliers } from 'src/test/data/replenishmentSuppliers';
import { LogiscticTable, TimeSlotModal } from './components';
import { resolveCurrentUser } from 'src/test/commonResolve';
import { logisticsParams } from 'src/test/data/logistics';
import { timeSlots } from 'src/test/data/timeSlots';
import { setupTestApp } from 'src/test/setupApp';
import { Button } from 'src/components';
import Api from 'src/Api';
import { waitForPromises } from 'src/test/utils/utils';

const testSupplier = suppliers[0];
const ffWarehouses = warehouses.filter(w => w.type === WarehouseType.FULFILLMENT);
const ffWarehousesId = ffWarehouses.map(w => w.id);
const fLogisticsParams = logisticsParams.filter(l => ffWarehousesId.includes(l.warehouseId));

let app: ReactWrapper;
let api: MockedApiObject<Api>;
let store: Store;

describe('LogisticParametersPage', () => {
  const expectRow = (row: ReactWrapper, warehouse: Warehouse, logisticParams: Record<number, LogisticsParamDTO>) => {
    expect(row.prop('warehouse')).toStrictEqual(warehouse);
    expect(row.prop('logisticParams')).toStrictEqual(logisticParams);
  };

  beforeEach(() => {
    ({ app, api, store } = setupTestApp(`/logisticsparams/${testSupplier.id}`));
  });

  it('renders page correctly', async () => {
    expect(app.find(LogisticParametersPage)).toHaveLength(1);

    resolveCurrentUser(api);

    app.find(LogisticParametersPage).prop('location').pathname = '/logisticsparams';
    const tableLoading = app.find(LogiscticTable);
    expect(tableLoading).toHaveLength(1);

    const firstEmptyRow = tableLoading.find(LogiscticTable).find(LogisticTableRowItem).get(0);
    expect(firstEmptyRow).toBeUndefined();
    expect(tableLoading.html()).toContain('Загружаем логистические параметры...');

    api.replenishmentWarehouseController.getWarehouses.next().resolve(ffWarehouses);
    api.replenishmentSupplierController.getSupplier.next().resolve(testSupplier);
    api.logisticsParamController.get.next().resolve(fLogisticsParams);
    api.suppliersScheduleController.getSupplierSchedules.next().resolve(timeSlots);
    store.dispatch(
      replenishmentActions.loadWarehouses.done({
        params: [WarehouseType.FULFILLMENT],
        result: ffWarehouses,
      })
    );

    await waitForPromises();
    app.update();

    const table = app.find(LogiscticTable);
    expect(table).toHaveLength(1);

    const rows = table.first().find(LogisticTableRowItem);
    expect(rows).toHaveLength(3);

    rows.forEach((row, index) => {
      expectRow(row, ffWarehouses[index], { [index + 2]: fLogisticsParams[index] });
    });

    const modals = app.find(TimeSlotModal);
    expect(modals).toHaveLength(ffWarehouses.length);

    let button = modals.at(0).find(Button);
    expect(button).toHaveLength(1);
    act(() => {
      button.simulate('click');
    });

    app.update();
    let weekDays = app.find(WeekDay);
    expect(weekDays).toHaveLength(7);

    // Первый временной слот, на первый склад
    expect(weekDays.at(0).find(TimeSlot)).toHaveLength(0);

    // expect(weekDays.find(TimeSlot).exists()).toBeTruthy()

    // Первый временной слот, на второй склад
    expect(weekDays.at(2).find(TimeSlot)).toHaveLength(1);

    // Выход из модала
    act(() => {
      app.simulate('keypress', { key: 'Escape' });
    });

    app.update();

    button = modals.at(1).find(Button);
    expect(button).toHaveLength(1);
    act(() => {
      button.simulate('click');
    });

    app.update();
    weekDays = app.find(WeekDay);

    // Старый модал просто сворачивается, а не убивается, для сохранения состояния редактирования,
    // так что когда модала два, число дней недели, рендерящихся в них, тоже удваивается
    expect(weekDays).toHaveLength(7 * 2);

    // Первый временной слот, на первый склад
    expect(weekDays.at(7).find(TimeSlot)).toHaveLength(0);

    // Первый временной слот, на второй склад
    expect(weekDays.at(10).find(TimeSlot)).toHaveLength(0);

    app.unmount();
  });
});
