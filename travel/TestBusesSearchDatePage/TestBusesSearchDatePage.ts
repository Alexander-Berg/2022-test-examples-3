import {SECOND} from 'helpers/constants/dates';

import TestBusesSearchForm from 'helpers/project/buses/components/TestBusesSearchForm/TestBusesSearchForm';
import TestBusesSearchSegments from 'helpers/project/buses/components/TestBusesSearchSegments/TestBusesSearchSegments';
import TestBusesEmptySerp from 'helpers/project/buses/components/TestBusesEmptySerp';

import {Component} from 'components/Component';
import TestLayoutDefault from 'components/TestLayoutDefault/TestLayoutDefault';

export default class TestBusesSearchDatePage extends TestLayoutDefault {
    searchForm: TestBusesSearchForm;
    title: Component;
    sortsDesktop: Component;
    filtersDesktop: Component;
    filtersAndSortsMobile: Component;
    segments: TestBusesSearchSegments;
    emptySerp: TestBusesEmptySerp;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'busesSearchDatePage');

        this.searchForm = new TestBusesSearchForm(browser);
        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });
        this.sortsDesktop = new Component(browser, {
            parent: this.qa,
            current: 'sortsDesktop',
        });
        this.filtersDesktop = new Component(browser, {
            parent: this.qa,
            current: 'filtersDesktop',
        });
        this.filtersAndSortsMobile = new Component(browser, {
            parent: this.qa,
            current: 'filtersAndSortsMobile-toggleButton',
        });
        this.segments = new TestBusesSearchSegments(browser, {
            parent: this.qa,
            current: 'segments',
        });

        this.emptySerp = new TestBusesEmptySerp(browser);
    }

    async waitUntilLoaded(): Promise<true | void> {
        return this.browser.waitUntil(
            async () => {
                return !(await this.segments.skeletons.items).length;
            },
            {timeout: 40 * SECOND},
        );
    }

    /**
     * Пустая ли страница поиска
     */
    async isEmptySearchPage(): Promise<boolean> {
        return !(await this.title.isVisible());
    }
}
