import {Component} from 'components/Component';

import {TestTrainsCoachTab} from './TestTrainsCoachTab';
import {TestTrainsPlacesViewTypeTabs} from './TestTrainsPlacesViewTypeTabs';

export class TestTrainsPlacesViewType extends Component {
    placesViewTypeTabs: TestTrainsPlacesViewTypeTabs;
    coachTab: TestTrainsCoachTab;
    requirements: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.placesViewTypeTabs = new TestTrainsPlacesViewTypeTabs(browser, {
            parent: this.qa,
            current: 'tabs',
        });

        this.coachTab = new TestTrainsCoachTab(browser, {
            parent: this.qa,
            current: 'coaches',
        });
        this.requirements = new Component(browser, {
            parent: this.qa,
            current: 'requirements',
        });
    }
}
