import TestPlacesItem from 'helpers/project/trains/components/TestOrderSummary/components/TestTrainItem/components/TestPlaces/components/TestPlacesItem';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export default class TestPlaces extends Component {
    places: ComponentArray<TestPlacesItem>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.places = new ComponentArray(
            browser,
            {parent: this.qa, current: 'placeItem'},
            TestPlacesItem,
        );
    }
}
