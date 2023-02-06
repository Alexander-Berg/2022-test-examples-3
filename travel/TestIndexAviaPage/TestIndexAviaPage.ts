import {
    AviaSearchForm,
    IAviaSearchFormParams,
} from 'helpers/project/avia/components/AviaSearchForm';

import {IndexTabs} from 'components/IndexTabs';
import {Component} from 'components/Component';
import {AdFoxBanner} from 'components/AdFoxBanner';
import {TestHeader} from 'components/TestHeader';
import TestCrossLinksGallery from 'components/TestCrossLinksGallery/TestCrossLinksGallery';

export class TestIndexAviaPage extends Component {
    searchForm: AviaSearchForm;
    indexTabs: IndexTabs;
    adfoxBanner: AdFoxBanner;
    header: TestHeader;
    crossLinksGallery: TestCrossLinksGallery;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'indexAviaPage');

        this.searchForm = new AviaSearchForm(browser);

        this.crossLinksGallery = new TestCrossLinksGallery(this.browser, {
            parent: this.qa,
            current: 'crossLinksGallery',
        });

        this.indexTabs = new IndexTabs(browser);

        this.adfoxBanner = new AdFoxBanner(browser);

        this.header = new TestHeader(browser);
    }

    async search(params: IAviaSearchFormParams): Promise<void> {
        await this.searchForm.fill(params);
        await this.searchForm.submitForm();
    }

    async goIndexPage(): Promise<void> {
        await this.browser.url('/');
    }
}
