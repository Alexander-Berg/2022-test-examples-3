import {HotelsSearchForm} from 'helpers/project/hotels/components/HotelsSearchForm';

import {Component} from 'components/Component';
import {Button} from 'components/Button';

export class TestCityPageHotelModal extends Component {
    searchForm: HotelsSearchForm;
    submitButton: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.searchForm = new HotelsSearchForm(browser, {
            parent: this.qa,
            current: 'searchForm',
        });

        this.submitButton = new Button(browser, {
            parent: this.qa,
            current: 'searchForm-submit',
        });
    }
}
