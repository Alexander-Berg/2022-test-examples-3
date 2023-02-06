import type {Ctx} from 'client.next/pages/SortingCenterOrderList/spec/e2e';

export const DATE_NOT_SPECIFIED = 'Invalid date';

export const checkDefaultFilters = async (ctx: Ctx) => {
    await ctx
        .expect(
            ctx.app.filterPanel.shippedToCourierDate.getCurrentPeriod(ctx.app.filterPanel.shippedToCourierDateButton),
        )
        .deep.equal(
            {
                from: DATE_NOT_SPECIFIED,
                to: DATE_NOT_SPECIFIED,
            },
            'Дефолтное значение для shippedToCourierDate без даты',
        );

    await ctx
        .expect(ctx.app.filterPanel.orderId.getValue())
        .to.equal('', 'Дефолтное значение для orderId пустая строка');

    await ctx
        .expect(ctx.app.filterPanel.cellNumber.getValue())
        .to.equal('', 'Дефолтное значение для cellNumber пустая строка');

    await ctx
        .expect(ctx.app.filterPanel.state.getSelectedI18n())
        .to.equal('common.sorting-center:filter.all', 'Дефолтное значение в фильтре по типу статуса заказа "Все"');

    await ctx
        .expect(ctx.app.filterPanel.courierName.getValue())
        .to.equal('', 'Дефолтное значение для courierName пустая строка');

    await ctx
        .expect(ctx.app.filterPanel.dispatchPersonName.getValue())
        .to.equal('', 'Дефолтное значение для dispatchPersonName пустая строка');

    await ctx.expect(ctx.app.filterPanel.resetButton.isVisible()).to.equal(true, 'Отображается кнопка сброса фильтров');
};
