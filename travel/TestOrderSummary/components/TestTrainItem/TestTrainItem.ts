import TestPlaces from 'helpers/project/trains/components/TestOrderSummary/components/TestTrainItem/components/TestPlaces/TestPlaces';
import {TestBedding} from 'helpers/project/trains/components/TestOrderSummary/components/TestTrainItem/components/TestBedding/TestBedding';

import {Component} from 'components/Component';

export default class TestTrainItem extends Component {
    title: Component;
    placesPlaceholder: Component;
    places: TestPlaces;
    bedding: TestBedding;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });

        this.placesPlaceholder = new Component(browser, {
            parent: this.qa,
            current: 'placesPlaceholder',
        });

        this.places = new TestPlaces(browser, {
            parent: this.qa,
            current: 'places',
        });

        this.bedding = new TestBedding(browser, {
            parent: this.qa,
            current: 'bedding',
        });
    }
}
