import {URL} from 'url';

import {TestTrainsInlineFilters} from 'helpers/project/trains/components/TestTrainsInlineFilters';
import {TestTrainsMobileFilters} from 'helpers/project/trains/components/TestTrainsMobileFilters';
import {TestTrainsSearchHeader} from 'helpers/project/trains/components/TestTrainsSearchHeader';
import {TestTrainsSearchFooter} from 'helpers/project/trains/components/TestTrainsSearchFooter';
import {TestTrainsBreadcrumbs} from 'helpers/project/trains/components/TestTrainsBreadcrumbs';
import {RailwaysSearchForm} from 'helpers/project/trains/components/RailwaysSearchForm';
import {TestTrainsSearchToolbar} from 'helpers/project/trains/components/TestTrainsSearchToolbar';
import {TestTrainsBaseSearchPage} from 'helpers/project/trains/pages/TestTrainsBaseSearchPage';

import {TestHeader} from 'components/TestHeader';
import {TestFooter} from 'components/TestFooter';
import {Component} from 'components/Component';
import {TestSearchHotelsCrossSaleMap} from 'components/TestSearchHotelsCrossSaleMap';

export class TestTrainsGenericSearchPage extends TestTrainsBaseSearchPage {
    filters: TestTrainsMobileFilters | TestTrainsInlineFilters;
    portalHeader: TestHeader;
    searchForm: RailwaysSearchForm;
    searchHeader: TestTrainsSearchHeader;
    searchToolbar: TestTrainsSearchToolbar;
    variantsDateSeparator: Component;
    searchFooter: TestTrainsSearchFooter;
    breadcrumbs: TestTrainsBreadcrumbs;
    footer: TestFooter;
    preloader: Component;
    notificationBanner: Component;
    crossSaleMap: TestSearchHotelsCrossSaleMap;

    constructor(browser: WebdriverIO.Browser, qa?: QA) {
        super(browser, qa);

        this.filters = this.isTouch
            ? new TestTrainsMobileFilters(browser)
            : new TestTrainsInlineFilters(browser);
        this.portalHeader = new TestHeader(browser);
        this.searchForm = new RailwaysSearchForm(browser);
        this.searchHeader = new TestTrainsSearchHeader(browser);
        this.searchToolbar = new TestTrainsSearchToolbar(
            browser,
            'searchToolbar',
        );
        this.variantsDateSeparator = new Component(
            browser,
            'variantsDateSeparator',
        );
        this.preloader = new Component(browser, 'searchPreloader');
        this.searchFooter = new TestTrainsSearchFooter(browser);
        this.breadcrumbs = new TestTrainsBreadcrumbs(browser);
        this.footer = new TestFooter(browser);
        this.notificationBanner = new Component(browser, 'notificationBanner');
        this.crossSaleMap = new TestSearchHotelsCrossSaleMap(
            browser,
            'hotelsCrossSaleMap',
        );
    }

    async isOpened(from: string, to: string): Promise<boolean> {
        const currentUrl = await this.browser.getUrl();
        const {pathname} = new URL(currentUrl);

        return pathname === `/trains/${from}--${to}/`;
    }
}
