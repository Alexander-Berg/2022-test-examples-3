import {MOCKED_COURIERS, MOCKED_ORDER_LIST} from '~/pages/SortingCenterOrderList/spec/mocks';
import type {Ctx} from 'client.next/pages/SortingCenterOrderList/spec/e2e';
import {checkDefaultFilters} from './checkDefaultFilters';

export const checkCourierNameFilter = (ctx: Ctx) => async () => {
    const courierName = MOCKED_COURIERS[0].name;

    await ctx.step(`Выставляем в фильтре courierName значение ${courierName} и проверяем таблицу`, async () => {
        const expectedCount = MOCKED_ORDER_LIST.filter(order => order.courierName === courierName).length;

        await ctx.app.filterPanel.courierName.setValue({value: courierName});
        await ctx.app.filterPanel.courierName.clickOnItemByName(courierName);

        await ctx.app.table.waitForUpdating();

        await ctx
            .expect(ctx.app.table.getRow(0).getCourierName())
            .to.equal(courierName, 'Таблица правильно отфильтровалась по courierName');

        await ctx
            .expect(ctx.app.table.inner.getRowCount())
            .to.equal(expectedCount, 'В таблице только заказы с указаным courierName');
    });

    await ctx.step('Сбрасываем фильтры', async () => {
        await ctx.app.filterPanel.resetFilters();

        await checkDefaultFilters(ctx);
    });
};
