import {MOCKED_ORDER_LIST} from '~/pages/SortingCenterOrderList/spec/mocks';
import type {Ctx} from 'client.next/pages/SortingCenterOrderList/spec/e2e';
import {checkDefaultFilters} from './checkDefaultFilters';

export const checkOrderIdFilter = (ctx: Ctx) => async () => {
    const orderId = MOCKED_ORDER_LIST[1].id;

    await ctx.step(`Выставляем в фильтре orderId значение ${orderId} и проверяем таблицу`, async () => {
        await ctx.app.filterPanel.orderId.setValue({
            value: orderId,
            name: 'orderId',
        });

        await ctx.app.table.waitForUpdating();

        await ctx
            .expect(ctx.app.table.getRow(0).getId())
            .to.equal(orderId, 'Таблица правильно отфильтровалась по orderId');

        await ctx
            .expect(ctx.app.table.inner.getRowCount())
            .to.equal(1, 'В таблице может быть только один заказ с указанным id');
    });

    await ctx.step('Сбрасываем фильтры', async () => {
        await ctx.app.filterPanel.resetFilters();

        await checkDefaultFilters(ctx);
    });
};
