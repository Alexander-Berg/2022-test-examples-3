import TestBusesSearchForm from 'helpers/project/buses/components/TestBusesSearchForm/TestBusesSearchForm';
import TestBusesAdvantages from 'helpers/project/buses/pages/TestBusesIndexPage/components/TestBusesAdvantages/TestBusesAdvantages';
import TestBusesHowToBuyATicket from 'helpers/project/buses/pages/TestBusesIndexPage/components/TestBusesHowToBuyATicket/TestBusesHowToBuyATicket';

import {Component} from 'components/Component';
import {AdFoxBanner} from 'components/AdFoxBanner';
import {IndexTabs} from 'components/IndexTabs';

export default class TestBusesIndexPage extends Component {
    searchForm: TestBusesSearchForm;
    previousSearches: Component;
    title: Component;
    advantages: TestBusesAdvantages;
    howToBuyATicket: TestBusesHowToBuyATicket;
    adfoxBanner: AdFoxBanner;
    indexTabs: IndexTabs;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'indexBusesPage') {
        super(browser, qa);

        this.searchForm = new TestBusesSearchForm(browser);
        this.previousSearches = new Component(browser, {
            parent: this.qa,
            current: 'previousSearches',
        });
        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });
        this.advantages = new TestBusesAdvantages(browser, {
            parent: this.qa,
            current: 'advantage',
        });
        this.howToBuyATicket = new TestBusesHowToBuyATicket(browser, {
            parent: this.qa,
            current: 'howToBuyATicket',
        });

        this.adfoxBanner = new AdFoxBanner(browser);
        this.indexTabs = new IndexTabs(browser);
    }
}
