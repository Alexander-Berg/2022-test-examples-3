import TestTripImage from 'helpers/project/trips/components/TestTripImage';

import {Component} from 'components/Component';
import {TestLink} from 'components/TestLink';

export default class TestActiveTripsListItem extends TestLink {
    readonly image: TestTripImage;
    readonly title: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.image = new TestTripImage(browser, {
            parent: this.qa,
            current: 'image',
        });

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });
    }
}
