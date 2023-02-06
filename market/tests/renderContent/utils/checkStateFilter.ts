import {MOCKED_ORDER_LIST} from '~/pages/SortingCenterOrderList/spec/mocks';
import type {Ctx} from 'client.next/pages/SortingCenterOrderList/spec/e2e';
import {assertI18nKeyDeep} from 'spec/utils';

import {checkDefaultFilters} from './checkDefaultFilters';

export const checkStateFilter = (ctx: Ctx) => async () => {
    const state = MOCKED_ORDER_LIST[0].state;

    await ctx.step(`Выставляем в фильтре state значение ${state} и проверяем таблицу`, async () => {
        const expectedCount = MOCKED_ORDER_LIST.filter(order => order.state === state).length;

        await ctx.app.filterPanel.state.selectAndClickItemByValue(state);

        await ctx.app.table.waitForUpdating();

        await assertI18nKeyDeep(ctx, {
            element: ctx.app.table.getRow(0).state,
            expectedKey: `common.sorting-center:order-state.${state}`,
            message: 'Таблица правильно отфильтровалась по status',
        });

        await ctx
            .expect(ctx.app.table.inner.getRowCount())
            .to.equal(expectedCount, 'В таблице только заказы с указаным state');
    });

    await ctx.step('Сбрасываем фильтры', async () => {
        await ctx.app.filterPanel.resetFilters();

        await checkDefaultFilters(ctx);
    });
};
