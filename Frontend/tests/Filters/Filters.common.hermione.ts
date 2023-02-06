import { checkFilter } from './helpers';

describe('SearchPage', function() {
    describe('Фильтр по бренду', function() {
        const filtersSelector = '.ProductListControls';
        const vendorFilterSelector = '.ProductListControlsButton_type_vendor';

        const vendorFilterPopupSelector = '.ProductListEnumFilter-Popup';
        const vendorFilterModalSelector = '.Modal .ProductListEnumFilter-Content';
        const firstVendorSelector = '.ProductListEnumFilter-Item';
        const secondVendorSelector = '.ProductListEnumFilter-Item:nth-child(2)';
        const searchInputSelector = '.ProductListEnumFilter-Content .Textinput-Control';
        const submitSelector = '.ProductListEnumFilter-ButtonGroup .Button2_view_action';
        const clearSelector = '.ProductListEnumFilter-ButtonGroup .FilterButtonGroup-Button:nth-child(1)';

        it('Раскрытие фильтра', async function() {
            const { browser } = this;
            const isDesktop = await browser.getMeta('platform') === 'desktop';

            await browser.yaOpenPageByUrl('/products/search?text=телефон&promo=nomooa&exp_flags=all_filters=0;all_filters_desktop=0');
            await browser.yaWaitForVisible(filtersSelector);

            await browser.assertView('plain', filtersSelector);

            await browser.click(vendorFilterSelector);
            await browser.yaWaitForVisible(isDesktop ? vendorFilterPopupSelector : vendorFilterModalSelector);
            await browser.yaCheckBaobabEvent({ path: '$page.$main.product-list-controls.product-list-vendor-filter.product-list-controls-button' });

            await browser.assertView('expanded', isDesktop ?
                [vendorFilterSelector, vendorFilterPopupSelector] :
                vendorFilterModalSelector
            );

            await browser.click(firstVendorSelector);
            await browser.yaCheckBaobabEvent({ path: '$page.$main.product-list-controls.product-list-vendor-filter' });

            await browser.assertView('checked', isDesktop ?
                [vendorFilterSelector, vendorFilterPopupSelector] :
                vendorFilterModalSelector
            );
        });

        hermione.skip.in('appium-chrome-phone', 'не умеет делать setValue()');
        it('Работает фильтрация списка брендов', async function() {
            const { browser } = this;
            const isDesktop = await browser.getMeta('platform') === 'desktop';

            await browser.yaOpenPageByUrl('/products/search?text=телефон&promo=nomooa&exp_flags=all_filters=0;all_filters_desktop=0');
            await browser.yaWaitForVisible(filtersSelector);

            await browser.click(vendorFilterSelector);
            await browser.yaWaitForVisible(isDesktop ? vendorFilterPopupSelector : vendorFilterModalSelector);

            await browser.setValue(searchInputSelector, 'App');
            await browser.yaWaitForHidden(secondVendorSelector);
        });

        it('Обновляет выдачу после выбора значения', async function() {
            const { browser } = this;
            const isDesktop = await browser.getMeta('platform') === 'desktop';

            await browser.yaOpenPageByUrl('/products/search?text=телефон&promo=nomooa&exp_flags=all_filters=0;all_filters_desktop=0');
            await browser.yaWaitForVisible(filtersSelector);

            await browser.click(vendorFilterSelector);
            await browser.yaWaitForVisible(isDesktop ? vendorFilterPopupSelector : vendorFilterModalSelector);

            await checkFilter(browser, '$page.$main.product-list-controls.product-list-vendor-filter', async() => {
                await browser.click(firstVendorSelector);
                await browser.click(submitSelector);
            });
        });

        it('Обновляет выдачу после сброса выбранного значения', async function() {
            const { browser } = this;
            const isDesktop = await browser.getMeta('platform') === 'desktop';

            await browser.yaOpenPageByUrl('/products/search?text=телефон&glfilter=7893318:10500168&promo=nomooa&exp_flags=all_filters=0;all_filters_desktop=0');
            await browser.yaWaitForVisible(filtersSelector);

            await browser.click(vendorFilterSelector);
            await browser.yaWaitForVisible(isDesktop ? vendorFilterPopupSelector : vendorFilterModalSelector);

            await checkFilter(browser, '$page.$main.product-list-controls.product-list-vendor-filter', async() => {
                await browser.click(clearSelector);
            });
        });
    });
});
