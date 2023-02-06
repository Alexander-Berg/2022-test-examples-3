import {MOCKED_DISPATCH_PERSONS, MOCKED_ORDER_LIST} from '~/pages/SortingCenterOrderList/spec/mocks';
import type {Ctx} from 'client.next/pages/SortingCenterOrderList/spec/e2e';
import {checkDefaultFilters} from './checkDefaultFilters';

export const checkDispatchPersonNameFilter = (ctx: Ctx) => async () => {
    const dispatchPersonName = MOCKED_DISPATCH_PERSONS[0].name;

    await ctx.step(
        `Выставляем в фильтре dispatchPersonName значение ${dispatchPersonName} и проверяем таблицу`,
        async () => {
            const expectedCount = MOCKED_ORDER_LIST.filter(order => order.dispatchPersonName === dispatchPersonName)
                .length;

            await ctx.app.filterPanel.dispatchPersonName.setValue({value: dispatchPersonName});
            await ctx.app.filterPanel.courierName.clickOnItemByName(dispatchPersonName);

            await ctx.app.table.waitForUpdating();

            await ctx
                .expect(ctx.app.table.inner.getRowCount())
                .to.equal(expectedCount, 'В таблице только заказы с указаным dispatchPersonName');
        },
    );

    await ctx.step('Сбрасываем фильтры', async () => {
        await ctx.app.filterPanel.resetFilters();

        await checkDefaultFilters(ctx);
    });
};
