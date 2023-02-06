import {Component} from 'components/Component';

export class TestHotelsSearchPageFiltersBar extends Component {
    quickFilters: Component;
    allFiltersButton: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.quickFilters = new Component(browser, {
            parent: this.qa,
            current: 'quickFilters',
        });
        this.allFiltersButton = new Component(browser, {
            parent: this.qa,
            current: 'allFiltersButton',
        });
    }
}
