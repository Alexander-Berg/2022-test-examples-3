import {URL} from 'url';

import {RailwaysSearchForm} from 'helpers/project/trains/components/RailwaysSearchForm';

import {Component} from 'components/Component';
import {AdFoxBanner} from 'components/AdFoxBanner';
import {IndexTabs} from 'components/IndexTabs';
import TestCrossLinksGallery from 'components/TestCrossLinksGallery/TestCrossLinksGallery';

export class TestIndexTrainsPage extends Component {
    searchForm: RailwaysSearchForm;
    adfoxBanner: AdFoxBanner;
    indexTabs: IndexTabs;
    crossLinksGallery: TestCrossLinksGallery;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'indexTrainsPage');

        this.searchForm = new RailwaysSearchForm(browser);

        this.crossLinksGallery = new TestCrossLinksGallery(this.browser, {
            parent: this.qa,
            current: 'crossLinksGallery',
        });

        this.adfoxBanner = new AdFoxBanner(browser);

        this.indexTabs = new IndexTabs(browser);
    }

    async isOpened(): Promise<boolean> {
        const currentUrl = await this.browser.getUrl();
        const {pathname} = new URL(currentUrl);

        return pathname === '/trains/';
    }
}
