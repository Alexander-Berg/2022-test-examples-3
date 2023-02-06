import {HotelsSearchForm} from 'helpers/project/hotels/components/HotelsSearchForm';

import {Component} from 'components/Component';
import {AdFoxBanner} from 'components/AdFoxBanner';
import {IndexTabs} from 'components/IndexTabs';
import TestCrossLinksGallery from 'components/TestCrossLinksGallery/TestCrossLinksGallery';

export class TestIndexHotelsPage extends Component {
    searchForm: HotelsSearchForm;
    adfoxBanner: AdFoxBanner;
    indexTabs: IndexTabs;
    crossLinksGallery: TestCrossLinksGallery;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'indexHotelsPage');

        this.searchForm = new HotelsSearchForm(browser);

        this.crossLinksGallery = new TestCrossLinksGallery(this.browser, {
            parent: this.qa,
            current: 'crossLinksGallery',
        });

        this.adfoxBanner = new AdFoxBanner(browser);

        this.indexTabs = new IndexTabs(browser);
    }
}
