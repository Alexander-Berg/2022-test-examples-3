import TestCoachTabItem from 'helpers/project/trains/components/TestTrainsPlacesViewType/TestCoachTabItem';

import {ComponentArray} from 'components/ComponentArray';
import {Component} from 'components/Component';

export class TestTrainsCoachTab extends Component {
    coaches: ComponentArray<TestCoachTabItem>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.coaches = new ComponentArray(
            browser,
            {parent: this.qa, current: 'coach'},
            TestCoachTabItem,
        );
    }
}
