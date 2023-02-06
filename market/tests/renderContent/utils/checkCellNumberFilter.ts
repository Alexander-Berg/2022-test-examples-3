import {MOCKED_ORDER_LIST} from '~/pages/SortingCenterOrderList/spec/mocks';
import type {Ctx} from 'client.next/pages/SortingCenterOrderList/spec/e2e';
import {checkDefaultFilters} from './checkDefaultFilters';

export const checkCellNumberFilter = (ctx: Ctx) => async () => {
    const cellNumber = MOCKED_ORDER_LIST[0].cellNumber;

    await ctx.step(`Выставляем в фильтре cellNumber значение ${cellNumber} и проверяем таблицу`, async () => {
        const expectedCount = MOCKED_ORDER_LIST.filter(order => order.cellNumber === cellNumber).length;

        await ctx.app.filterPanel.cellNumber.setValue({
            value: cellNumber,
            name: 'cellNumber',
        });

        await ctx.app.table.waitForUpdating();

        await ctx
            .expect(ctx.app.table.getRow(0).getCellNumber())
            .to.equal(cellNumber, 'Таблица правильно отфильтровалась по cellNumber');

        await ctx
            .expect(ctx.app.table.inner.getRowCount())
            .to.equal(expectedCount, 'В таблице только заказы с указаным cellNumber');
    });

    await ctx.step('Сбрасываем фильтры', async () => {
        await ctx.app.filterPanel.resetFilters();

        await checkDefaultFilters(ctx);
    });
};
