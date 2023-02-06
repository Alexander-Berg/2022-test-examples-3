import TestBusesSearchForm from 'helpers/project/buses/components/TestBusesSearchForm/TestBusesSearchForm';

import {Component} from 'components/Component';
import {AdFoxBanner} from 'components/AdFoxBanner';
import {IndexTabs} from 'components/IndexTabs';
import TestCrossLinksGallery from 'components/TestCrossLinksGallery/TestCrossLinksGallery';
import TestAdvantages from 'components/TestAdvantages/TestAdvantages';

export default class TestBusesIndexPage extends Component {
    searchForm: TestBusesSearchForm;
    previousSearches: Component;
    advantages: TestAdvantages;
    howToBuyATicket: TestAdvantages;
    adfoxBanner: AdFoxBanner;
    indexTabs: IndexTabs;
    crossLinksGallery: TestCrossLinksGallery;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'indexBusesPage');

        this.searchForm = new TestBusesSearchForm(browser);
        this.previousSearches = new Component(browser, {
            parent: this.qa,
            current: 'previousSearches',
        });

        this.advantages = new TestAdvantages(browser, {
            parent: this.qa,
            current: 'advantages',
        });

        this.crossLinksGallery = new TestCrossLinksGallery(this.browser, {
            parent: this.qa,
            current: 'crossLinksGallery',
        });

        this.howToBuyATicket = new TestAdvantages(browser, {
            parent: this.qa,
            current: 'howToBuyATicket',
        });

        this.adfoxBanner = new AdFoxBanner(browser);
        this.indexTabs = new IndexTabs(browser);
    }
}
