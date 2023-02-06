import {travellineOfferPage} from 'suites/hotels';

import {TRAVELLINE_OFFER_PAGE_QA_PREFIX} from 'helpers/project/hotels/pages/TestTravellineOfferPage/constants';

import {Component} from 'helpers/project/common/components';

import {TestCheckbox} from 'components/TestCheckbox';

export class TestTravellineOfferPage extends Component {
    logo: Component;
    checkbox: TestCheckbox;
    button: Component;

    constructor(browser: WebdriverIO.Browser) {
        super(browser);

        this.logo = new Component(browser, {
            parent: TRAVELLINE_OFFER_PAGE_QA_PREFIX,
            current: 'logo',
        });

        this.checkbox = new TestCheckbox(browser, {
            parent: TRAVELLINE_OFFER_PAGE_QA_PREFIX,
            current: 'checkbox',
        });

        this.button = new Component(browser, {
            parent: TRAVELLINE_OFFER_PAGE_QA_PREFIX,
            current: 'button',
        });
    }

    goToOfferPage(): Promise<string> {
        return this.browser.url(travellineOfferPage.url);
    }
}
