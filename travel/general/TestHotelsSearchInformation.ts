import {Component} from 'components/Component';

const SEARCH_PARAMS_DEFAULT_TIMEOUT = 5000;

export class TestHotelsSearchInformation extends Component {
    searchParams: Component;
    totalLabel: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.searchParams = new Component(browser, {
            parent: this.qa,
            current: 'searchParams',
        });
        this.totalLabel = new Component(browser, {
            parent: this.qa,
            current: 'totalLabel',
        });
    }

    getSearchParamsText(): Promise<string> {
        return this.searchParams.getText(SEARCH_PARAMS_DEFAULT_TIMEOUT);
    }

    getTotalLabelText(): Promise<string> {
        return this.totalLabel.getText(SEARCH_PARAMS_DEFAULT_TIMEOUT);
    }
}
