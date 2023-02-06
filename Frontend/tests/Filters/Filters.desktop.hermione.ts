import { checkFilter, prepareModalAssertView } from './helpers';

describe('SearchPage', function() {
    describe('Категорийные фильтры', function() {
        const filtersSelector = '.ProductListControls';
        const filtersControlSelector = '#filter.ProductListControlsButton';

        const filtersModalSelector = '.Filters-Modal';
        const filterModalBackSelector = '.Filters-HeaderButton_left';
        const filterModalCloseSelector = '.Filters-HeaderButton_right';
        const clearButtonSelector = '.FilterButtonGroup-Button:nth-child(1)';
        const submitButtonSelector = '.FilterButtonGroup-Button:nth-child(2)';

        const firstFilterSelector = '.Filters-ListItem:nth-child(1)';
        const secondFilterSelector = '.Filters-ListItem:nth-child(2)';

        const numberFilterSelector = '.Filters-NumberFilter';
        const enumFilterSelector = '.Filters-EnumFilter';
        const firstEnumFilterSelector = '.Filters-EnumFilterItem:nth-child(1)';

        const filterModalWrapperSelector = '.Modal-Wrapper';

        it('Внешний вид', async function() {
            const { browser } = this;

            await browser.yaOpenPageByUrl('/products/search?text=телефон&promo=nomooa&exp_flags=all_filters_desktop=1');
            await browser.yaWaitForVisible(filtersSelector);

            await browser.assertView('plain', filtersSelector);
        });

        it('Раскрытие фильтров', async function() {
            const { browser } = this;

            await browser.yaOpenPageByUrl('/products/search?text=телефон&promo=nomooa&exp_flags=all_filters_desktop=1');
            await browser.yaWaitForVisible(filtersSelector);

            await browser.click(filtersControlSelector);
            await browser.yaWaitForVisible(filtersModalSelector);
            await browser.yaCheckBaobabEvent({ path: '$page.$main.product-list-controls.button-open-filters-list' });

            await prepareModalAssertView(browser);
            await browser.assertView('plain', filtersModalSelector);
            await browser.yaScrollElement(filterModalWrapperSelector, 0, 100);
            await browser.assertView('plain-sticky-all', filtersModalSelector);
            await browser.yaScrollElement(filterModalWrapperSelector, 0, 2000);
            await browser.assertView('plain-sticky-top', filtersModalSelector);
            await browser.yaScrollElement(filterModalWrapperSelector, 0, 0);

            await browser.click(firstFilterSelector);
            await browser.yaWaitForVisible(numberFilterSelector);

            await prepareModalAssertView(browser);
            await browser.assertView('number-filter', filtersModalSelector);

            await browser.click(filterModalBackSelector);
            await browser.yaWaitForHidden(numberFilterSelector);

            await browser.click(secondFilterSelector);
            await browser.yaWaitForVisible(enumFilterSelector);

            await prepareModalAssertView(browser);
            await browser.assertView('enum-filter', filtersModalSelector);
            await browser.yaScrollElement(filterModalWrapperSelector, 0, 100);
            await browser.assertView('enum-filter-sticky-all', filtersModalSelector);
            await browser.yaScrollElement(filterModalWrapperSelector, 0, 6000);
            await browser.assertView('enum-filter-sticky-top', filtersModalSelector);

            await browser.yaScrollElement(filterModalWrapperSelector, 0, 0);
            await browser.click(filterModalCloseSelector);
            await browser.yaWaitForHidden(filtersModalSelector);
        });

        it('Обновляют выдачу после выбора значения', async function() {
            const { browser } = this;

            await browser.yaOpenPageByUrl('/products/search?text=телефон&promo=nomooa&exp_flags=all_filters_desktop=1');
            await browser.yaWaitForVisible(filtersSelector);

            await browser.click(filtersControlSelector);
            await browser.yaWaitForVisible(filtersModalSelector);

            await browser.click(secondFilterSelector);
            await browser.yaWaitForVisible(enumFilterSelector);

            await browser.click(firstEnumFilterSelector);
            await browser.yaWaitForVisible(submitButtonSelector);

            await browser.click(submitButtonSelector);
            await browser.yaWaitForHidden(enumFilterSelector);

            await checkFilter(browser, '$page.$main.product-list-controls.filters-modal.filters-list.button-group.button-submit', async() => {
                await browser.click(submitButtonSelector);
            });
        });

        it('Обновляют выдачу после сброса выбранного значения', async function() {
            const { browser } = this;

            await browser.yaOpenPageByUrl('/products/search?text=телефон&promo=nomooa&glfilter=7893318:10500168&exp_flags=all_filters_desktop=1');
            await browser.yaWaitForVisible(filtersSelector);

            await browser.assertView('plain', filtersSelector);

            await browser.click(filtersControlSelector);
            await browser.yaWaitForVisible(filtersModalSelector);

            await checkFilter(browser, '$page.$main.product-list-controls.filters-modal.filters-list.button-group.button-clear', async() => {
                await browser.click(clearButtonSelector);
            });
        });
    });
});
