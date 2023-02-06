interface ISetPriceFilterValues {
    from?: string;
    to?: string;
}

export async function setPriceFilter(
    bro: WebdriverIO.Browser,
    values: ISetPriceFilterValues,
    condition?: () => Promise<boolean>,
    timeoutMsg: string = 'не поменялись товары после изменения фильтра по цене',
) {
    const waitProductsOnPriceFilter = await bro.yaWaitElementsChanging(
        '.ProductCardsList-Wrapper .ProductCard',
        { condition, timeoutMsg },
    );

    await bro.click('.ProductListControlsButton_type_price');

    await bro.yaWaitForVisible('.ProductListPriceFilter-Content');
    if (typeof values.from !== 'undefined') {
        await bro.setValue('.ProductListPriceFilter-PriceInput:first-child input', values.from);

        await bro.waitUntil(async() => {
            const fromInputValue = await bro.getValue('.ProductListPriceFilter-PriceInput:first-child input');

            return fromInputValue === values.from;
        }, {
            timeout: 2000,
            timeoutMsg: 'Не поменялось значение "от"',
        });
    }
    if (typeof values.to !== 'undefined') {
        await bro.setValue('.ProductListPriceFilter-PriceInput:last-child input', values.to);

        await bro.waitUntil(async() => {
            const toInputValue = await bro.getValue('.ProductListPriceFilter-PriceInput:last-child input');

            return toInputValue === values.to;
        }, {
            timeout: 2000,
            timeoutMsg: 'Не поменялось значение "до"',
        });
    }
    await bro.keys('Enter');
    await bro.yaWaitForVisible('.FilterButtonGroup-Button.Button2_view_action');

    await bro.click('.FilterButtonGroup-Button.Button2_view_action');
    await bro.yaWaitForHidden('.ProductListPriceFilter-Content');
    await waitProductsOnPriceFilter();
}

export async function setSorting(
    bro: WebdriverIO.Browser,
    value: string,
    condition: () => Promise<boolean>,
    msg: string = 'не поменялись товары после изменения сортировки',
) {
    const waitProductsOnSorting = await bro.yaWaitElementsChanging(
        '.ProductCardsList-Wrapper .ProductCard',
        { condition, timeoutMsg: msg },
    );

    if (await bro.getMeta('platform') === 'desktop') {
        await bro.click('.ProductListControlsButton_type_sorting');
        await bro.yaWaitForVisible('.ProductListSorting .ProductListDropDown-Popup', 'список сортировки не появился');
        await bro.click(`.ProductListSorting .ProductListDropDown-PopupButton[value="${value}"]`);
    } else {
        await bro.click('.ProductListSorting .ProductListDropDown-Select');
        await bro.selectByAttribute('.ProductListSorting .ProductListDropDown-Select', 'value', value);
    }

    await bro.yaWaitForHidden('.ProductCardsList-Shadow', 3000, 'затенение не скрылось после применения сортировки');

    await waitProductsOnSorting();
}

export async function setUsedGoodsFilter(
    bro: WebdriverIO.Browser,
    value: string,
    condition: () => Promise<boolean>,
    msg: string = 'не поменялись товары после изменения фильтра б/у',
) {
    const waitProductsOnFilterApplying = await bro.yaWaitElementsChanging(
        '.ProductCardsList-Wrapper .ProductCard',
        { condition, timeoutMsg: msg },
    );

    if (await bro.getMeta('platform') === 'desktop') {
        await bro.click('.ProductListControlsButton_type_usedGoods');
        await bro.yaWaitForVisible('.ProductListUsedGoodsFilter .ProductListDropDown-Popup', 'список значений фильтра б/у не появился');
        await bro.click(`.ProductListUsedGoodsFilter .ProductListDropDown-PopupButton[value="${value}"]`);
    } else {
        await bro.click('.ProductListUsedGoodsFilter .ProductListDropDown-Select');
        await bro.selectByAttribute('.ProductListUsedGoodsFilter .ProductListDropDown-Select', 'value', value);
    }

    await bro.yaWaitForHidden('.ProductCardsList-Shadow', 3000, 'затенение не скрылось после применения фильтра по б/у');

    await waitProductsOnFilterApplying();
}
