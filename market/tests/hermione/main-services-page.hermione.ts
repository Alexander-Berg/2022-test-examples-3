import {expect} from 'chai';

import {MARKET_MAP_INDEX_URL} from '../../../../tsum-front-core/configs/routes';
import SearchBarPO from '../../src/features/market-map/components/ServicesSearchBar/__pageObject';
import ServicesTablePO from '../../src/features/market-map/containers/ServicesTable/__pageObject';
import FillStatsPO from '../../src/features/market-map/containers/FillStats/__pageObject';
import NodeCriticalSwitchPO from '../../src/features/market-map/components/NodeCriticalSwitch/__pageObject';
import {
    EXISTING_COMPONENT_DATA,
    EXISTING_SERVICE_DATA,
} from './constants';

describe('Главная страница Карты', () => {
    it('Отображение элементов главной страницы', async function () {
        const servicesTable = new ServicesTablePO(this.browser);
        const searchBar = new SearchBarPO(this.browser);

        await this.browser.loginAndRedirect(MARKET_MAP_INDEX_URL);
        await servicesTable.waitForExist();

        const searchBarExists = await searchBar.isVisible();
        const tableExists = await servicesTable.isVisible();

        expect(tableExists).to.equal(true);
        expect(searchBarExists).to.equal(true);
    });

    it('Разворачивание/сворачивание дерева компонентов', async function () {
        const servicesTable = new ServicesTablePO(this.browser);
        const {slug} = EXISTING_SERVICE_DATA;

        await this.browser.loginAndRedirect(MARKET_MAP_INDEX_URL);
        await servicesTable.waitForExist();

        const serviceElementsBefore = await servicesTable.serviceElements;
        const totalServicesCountBefore = serviceElementsBefore.length;

        const arrow = await servicesTable.getServiceElementCollapseArrow(slug);

        await arrow.click();

        const serviceElementsAfter = await servicesTable.serviceElements;
        const totalServicesCountAfter = serviceElementsAfter.length;

        expect(totalServicesCountAfter > totalServicesCountBefore).to.equal(true);
    });

    it('Поиск существующего сервиса', async function () {
        const searchBar = new SearchBarPO(this.browser);
        const servicesTable = new ServicesTablePO(this.browser);
        const {slug, query} = EXISTING_COMPONENT_DATA;

        await this.browser.loginAndRedirect(MARKET_MAP_INDEX_URL);
        await searchBar.waitForVisible();

        const field = await searchBar.searchBarField;
        await field.setValue(query);
        await this.browser.pause(500);

        const existingServiceElement = await servicesTable.getServiceElement(slug);
        const isExistingServiceElementVisible = await existingServiceElement.isDisplayed();
        const fillStats = new FillStatsPO(this.browser, existingServiceElement);

        const fillStatsIcon = await fillStats.fillStatsIcon;

        await fillStatsIcon.moveTo();
        await this.browser.pause(100);

        await fillStats.fillStatsPopoverContent.waitForExist();

        const fillStatsItems = await fillStats.fillStatsItems;

        expect(isExistingServiceElementVisible).to.equal(true);
        expect(fillStatsItems.length > 0).to.equal(true);
    });

    it('Поиск несуществующего сервиса', async function () {
        const searchBar = new SearchBarPO(this.browser);
        const servicesTable = new ServicesTablePO(this.browser);

        await this.browser.loginAndRedirect(MARKET_MAP_INDEX_URL);
        await searchBar.waitForVisible();

        const field = await searchBar.searchBarField;

        await field.setValue('$e@rch query');
        await this.browser.pause(500);

        const emptyResultPlaceholder = await servicesTable.emptyResultPlaceholder;
        let emptyResultPlaceholderVisible = false;

        if (emptyResultPlaceholder) {
            emptyResultPlaceholderVisible = await emptyResultPlaceholder.isDisplayed();
        }

        expect(emptyResultPlaceholderVisible).to.equal(true);
    });

    it('Сохранение параметров поиска', async function () {
        const searchBar = new SearchBarPO(this.browser);
        const nodeCriticalSwitch = new NodeCriticalSwitchPO(this.browser);

        await this.browser.loginAndRedirect(MARKET_MAP_INDEX_URL);
        await searchBar.waitForVisible();

        const field = await searchBar.searchBarField;

        await field.setValue('query');
        await nodeCriticalSwitch.toggle();
        await this.browser.pause(500);

        const browserUrl = await this.browser.url();
        const params = new URL(browserUrl).searchParams;

        expect(params.get('query')).to.equal('query');
        expect(params.get('isCritical')).to.equal('true');
    });

    it('Заполнение параметров поиска', async function () {
        const searchBar = new SearchBarPO(this.browser);
        const nodeCriticalSwitch = new NodeCriticalSwitchPO(this.browser);

        await this.browser.loginAndRedirect(`${MARKET_MAP_INDEX_URL}?query=query&isCritical=true`);
        await searchBar.waitForVisible();

        const field = await searchBar.searchBarField;

        const isCriticalSwitchActive = await nodeCriticalSwitch.getIsActive();
        const searchFieldValue = await field.getValue();

        expect(isCriticalSwitchActive).to.equal(true);
        expect(searchFieldValue).to.equal('query');
    });
});
