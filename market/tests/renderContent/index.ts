import {ISO_8601_DATE_FORMAT} from '@yandex-market/b2b-core/shared/constants/date';

import type {Ctx} from '~/pages/SortingCenterOrderList/spec/e2e';
import {MOCKED_ORDER_LIST} from '~/pages/SortingCenterOrderList/spec/mocks';
import {PLATFORM_TYPE} from 'shared/constants/campaign';
import dayjs from 'shared/utils/dayjs';
import {buildUrl} from 'spec/utils';

import {
    checkOrderIdFilter,
    checkCellNumberFilter,
    checkStateFilter,
    checkCourierNameFilter,
    checkDispatchPersonNameFilter,
    checkDefaultFilters,
} from './utils';

export const checkRenderContent = async (ctx: Ctx) => {
    await ctx.step('Отображаются фильтры', async () => {
        const currentDateFormatted = dayjs().format(ISO_8601_DATE_FORMAT);

        await ctx
            .expect(ctx.app.filterPanel.arrivedToSoDate.getCurrentPeriod(ctx.app.filterPanel.arrivedToSoDateButton))
            .deep.equal({
                from: currentDateFormatted,
                to: currentDateFormatted,
            });

        await checkDefaultFilters(ctx);
    });

    await ctx.step('Отображается заголовок таблицы', async () => {
        await ctx.app.tableHeader.title.waitForVisible();
        await ctx.expect(ctx.app.tableHeader.title.isVisible()).to.equal(true, 'Отображается title');
        await ctx.expect(ctx.app.tableHeader.title.getText()).to.match(/ \d+ /, 'title содержит число');

        await ctx
            .expect(ctx.app.tableHeader.csvButton.isVisible())
            .to.equal(true, 'Отображается кнопка для скачивания csv файла');
    });

    await ctx.step('Отображается контент', async () => {
        await ctx.app.filterPanel.setArrivedToSoDatePeriod('2020-10-02', '2020-10-04');

        await ctx.app.table.waitForUpdating();

        await ctx.expect(ctx.app.table.isVisible()).to.equal(true, 'Таблица с пользователями отображается');

        await ctx
            .expect(ctx.app.table.inner.getHeaderI18nKeys())
            .to.deep.equal(
                [
                    'pages.sorting-center-order-list:table.column.id',
                    'pages.sorting-center-order-list:table.column.arrived-to-so-date',
                    'pages.sorting-center-order-list:table.column.arrived-to-so-time',
                    'pages.sorting-center-order-list:table.column.ship-to-courier-date',
                    'pages.sorting-center-order-list:table.column.ship-to-courier-time',
                    'pages.sorting-center-order-list:table.column.cell-number',
                    'pages.sorting-center-order-list:table.column.courier-name',
                    'pages.sorting-center-order-list:table.column.state',
                    'pages.sorting-center-order-list:table.column.warehouse-name',
                    'pages.sorting-center-order-list:table.column.place-count',
                    'pages.sorting-center-order-list:table.column.lot-external-id',
                ],
                'В таблице заказов отображаются правильные колонки',
            );

        await ctx
            .expect(ctx.app.table.inner.getRowCount())
            .to.equal(MOCKED_ORDER_LIST.length, 'Отображается правильное количество строк в таблице');
    });

    await ctx.step('Проверяем фильтр orderId', checkOrderIdFilter(ctx));

    await ctx.step('Проверяем фильтр cellNumber', checkCellNumberFilter(ctx));
    await ctx.step('Проверяем фильтр state', checkStateFilter(ctx));
    await ctx.step('Проверяем фильтр courierName', checkCourierNameFilter(ctx));
    await ctx.step('Проверяем фильтр dispatchPersonName', checkDispatchPersonNameFilter(ctx));

    await ctx.step('Проверяем ссылку на кнопке "Выгрузить в CSV"', async () => {
        const expectedUrl = buildUrl('market-partner:file:sorting-center-order-list:get', {
            platformType: PLATFORM_TYPE.SORTING_CENTER,
            campaignId: ctx.params.shop.campaignId,
        });
        const href = await ctx.app.tableHeader.csvButton.root.getAttribute('href');

        await ctx.expect(href).to.contain(expectedUrl, 'Кнопка скачивания csv содержит правильный урл');
    });

    await ctx.step('Проверяем ссылку при переходе на страницу заказа', async () => {
        const {id} = MOCKED_ORDER_LIST[0];

        const expectedUrl = buildUrl('market-partner:html:sorting-center-order:get', {
            platformType: PLATFORM_TYPE.SORTING_CENTER,
            campaignId: ctx.params.shop.campaignId,
            orderId: id,
        });
        const href = await ctx.app.table.getRow(0).id.getAttribute('href');

        await ctx.expect(href).to.contain(expectedUrl, 'Ссылка перехода на страницу заказа содержит правильный урл');
    });
};
