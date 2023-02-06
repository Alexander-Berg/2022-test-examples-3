import { checkFilter } from './helpers';

describe('SearchPage', function() {
    describe('Категорийные фильтры', function() {
        const filtersSelector = '.ProductListControls';
        const filtersControlSelector = '#filter.ProductListControlsButton';

        const filtersModalSelector = '.Filters-Modal';
        const filterModalBackSelector = '.Filters-HeaderButton_left';
        const filterModalCloseSelector = '.Filters-HeaderButton_right';
        const clearButtonSelector = '.FilterButtonGroup-Button:nth-child(1)';
        const submitButtonSelector = '.FilterButtonGroup-Button:nth-child(2)';

        const listItemSelector = '.Filters-ListItem';

        const firstFilterSelector = '.Filters-ListItem:nth-child(1)';
        const secondFilterSelector = '.Filters-ListItem:nth-child(2)';
        const thirdFilterSelector = '.Filters-ListItem:nth-child(3)';

        const numberFilterSelector = '.Filters-NumberFilter';
        const enumFilterSelector = '.Filters-EnumFilter';
        const firstEnumFilterSelector = '.Filters-EnumFilterItem:nth-child(1)';

        const searchField = '.Filters-SearchField';
        const searchFieldInput = '.Filters-SearchField input';
        const searchFieldClear = '.Filters-SearchFieldClear';

        it('Внешний вид', async function() {
            const { browser } = this;

            await browser.yaOpenPageByUrl('/products/search?text=телефон&promo=nomooa');
            await browser.yaWaitForVisible(filtersSelector);

            await browser.assertView('plain', filtersSelector);
        });

        it('Внешний вид в одну строку', async function() {
            const { browser } = this;

            await browser.yaOpenPageByUrl('/products/search?text=телефон&promo=nomooa&exp_flags=all_filters_one_line');
            await browser.yaWaitForVisible(filtersSelector);

            await browser.assertView('plain', filtersSelector);

            await browser.yaOpenPageByUrl('/products/search?text=телефон&glfilter=7893318%3A153087%2C152863%2C12260898&glfilter=34812830%3A36106237&promo=nomooa&exp_flags=all_filters_one_line');
            await browser.yaWaitForVisible(filtersSelector);

            await browser.assertView('with_values', filtersSelector);
        });

        it('Раскрытие фильтров', async function() {
            const { browser } = this;

            await browser.yaOpenPageByUrl('/products/search?text=телефон&promo=nomooa');
            await browser.yaWaitForVisible(filtersSelector);

            await browser.click(filtersControlSelector);
            await browser.yaWaitForVisible(filtersModalSelector);
            await browser.yaCheckBaobabEvent({ path: '$page.$main.product-list-controls.button-open-filters-list' });

            await browser.assertView('plain', filtersModalSelector);

            await browser.click(firstFilterSelector);
            await browser.yaWaitForVisible(numberFilterSelector);

            await browser.assertView('number-filter', filtersModalSelector);

            await browser.click(filterModalBackSelector);
            await browser.yaWaitForHidden(numberFilterSelector);

            await browser.click(secondFilterSelector);
            await browser.yaWaitForVisible(enumFilterSelector);

            await browser.assertView('enum-filter', filtersModalSelector);

            await browser.click(filterModalCloseSelector);
            await browser.yaWaitForHidden(filtersModalSelector);
        });

        it('Обновляют выдачу после выбора значения', async function() {
            const { browser } = this;

            await browser.yaOpenPageByUrl('/products/search?text=телефон&promo=nomooa');
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

            await browser.yaOpenPageByUrl('/products/search?text=телефон&promo=nomooa&glfilter=7893318:10500168');
            await browser.yaWaitForVisible(filtersSelector);

            await browser.assertView('plain', filtersSelector);

            await browser.click(filtersControlSelector);
            await browser.yaWaitForVisible(filtersModalSelector);

            await checkFilter(browser, '$page.$main.product-list-controls.filters-modal.filters-list.button-group.button-clear', async() => {
                await browser.click(clearButtonSelector);
            });
        });

        hermione.skip.in('appium-chrome-phone', 'не умеет делать setValue()');
        it('Поисковое поле', async function() {
            const { browser } = this;

            await browser.yaOpenPageByUrl('/products/search?text=джинсы');
            await browser.yaWaitForVisible(filtersSelector);

            await browser.click(filtersControlSelector);
            await browser.yaWaitForVisible(filtersModalSelector);

            await browser.click(thirdFilterSelector);

            const initialCount = (await browser.$$(listItemSelector)).length;

            await browser.assertView('empty', searchField);

            await browser.setValue(searchFieldInput, 'xs');
            await browser.assertView('with-text', searchField);

            const searchedCount = (await browser.$$(listItemSelector)).length;

            assert.isBelow(searchedCount, initialCount, 'кол-во элементов не изменилось после поиска');

            await browser.click(searchFieldClear);

            const afterClearCount = (await browser.$$(listItemSelector)).length;

            assert.strictEqual(afterClearCount, initialCount, 'некорректное кол-во элементов после очистки поля поиска');
        });

        it('Добавляет параметр disauto после применения фильтров', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/search?text=телефон&promo=nomooa');
            await bro.yaWaitForVisible(filtersSelector);

            assert.isFalse(
                new URL(await bro.getUrl()).searchParams.has('disauto'),
                'изначально не должно быть параметра disauto',
            );

            await bro.click(filtersControlSelector);
            await bro.yaWaitForVisible(filtersModalSelector);

            await bro.click(secondFilterSelector);
            await bro.yaWaitForVisible(enumFilterSelector);

            await bro.click(firstEnumFilterSelector);
            await bro.yaWaitForVisible(submitButtonSelector);

            await bro.click(submitButtonSelector);
            await bro.yaWaitForHidden(enumFilterSelector);
            await bro.click(submitButtonSelector);

            assert.deepEqual(
                new URL(await bro.getUrl()).searchParams.getAll('disauto'),
                ['glfilter'],
                'должен установиться параметр disauto',
            );
        });
    });
});
