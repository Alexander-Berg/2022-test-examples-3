import TestForecastItem from 'helpers/project/account/pages/TripPage/components/TestForecastBlock/components/TestForecastItem';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export default class TestForecastBlock extends Component {
    items: ComponentArray<TestForecastItem>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.items = new ComponentArray(
            this.browser,
            {parent: this.qa, current: 'item'},
            TestForecastItem,
        );
    }
}
