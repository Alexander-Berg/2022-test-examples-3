import type {Ctx} from 'client.next/pages/SortingCenterOrderList/spec/e2e';
import {checkPagination as checkPaginationUtil} from 'spec/utils';
import TableRow from '../../../pageObjects/TableRow';

export const checkPagination = async (ctx: Ctx) => {
    await ctx.step(
        'Меняем значения фильтра по дате на такие, для которых есть заказы, ждем загрузки таблицы',
        async () => {
            await ctx.app.filterPanel.setArrivedToSoDatePeriod('2020-10-02', '2020-10-04');
            await ctx.app.table.waitForUpdating();
            await ctx.expect(ctx.app.table.isVisible()).to.equal(true, 'Таблица с пользователями отображается');
        },
    );

    await checkPaginationUtil(ctx, {
        pagerSize: ctx.app.pagerSize,
        pageSize: 20,
        pager: ctx.app.pager,
        table: ctx.app.table,
        getRowUniqueValue: (row: typeof TableRow) => row.id.getText(),
    });
};
