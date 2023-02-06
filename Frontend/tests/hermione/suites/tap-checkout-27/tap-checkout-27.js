const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-27.json');

// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('tap-checkout-27: Выбор города. Скролл списка с результатами поиска', function() {
    beforeEach(function() {
        this.defaultData = cloneDeep(defaultTestData);
    });

    it('Должна быть возможность просмотра полного списка результатов поиска', async function() {
        const bro = this.browser;
        const testData = this.defaultData;

        await checkoutPage.open(bro, testData);
        await bro.waitForVisible(checkoutPage.deliveryCityButton, 5000);

        await checkoutPage.openCityScreen(bro);
        await checkoutPage.fillCitySearchInput(bro, 'ер');
        await bro.waitForVisible(checkoutPage.screenCitySearchResultsItem, 10000);
        await bro.assertViewAfterLockFocusAndHover('deliveryCityResults', checkoutPage.root);

        await bro.scrollTop(checkoutPage.root, 1000);
        await bro.assertView('deliveryCityResultScrollUp', checkoutPage.root);
    });
});
