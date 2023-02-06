import TestTripImage from 'helpers/project/trips/components/TestTripImage';

import {Component} from 'components/Component';
import {TestLink} from 'components/TestLink';

export default class TestPastTripItem extends TestLink {
    readonly tripImage: TestTripImage;
    readonly title: Component;
    readonly displayDate: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.tripImage = new TestTripImage(browser, {
            parent: this.qa,
            current: 'tripImage',
        });

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });

        this.displayDate = new Component(browser, {
            parent: this.qa,
            current: 'displayDate',
        });
    }
}
