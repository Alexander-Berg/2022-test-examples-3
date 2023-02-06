import TestActivity from 'helpers/project/account/pages/TripPage/components/TestActivitiesBlock/components/TestActivity';
import TestActivityTypeFilter from 'helpers/project/account/pages/TripPage/components/TestActivitiesBlock/components/TestActivityTypeFilter';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export default class TestActivitiesBlock extends Component {
    title: Component;
    activities: ComponentArray<TestActivity>;
    activityTypeFilter: TestActivityTypeFilter;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(this.browser, {
            parent: this.qa,
            current: 'title',
        });
        this.activities = new ComponentArray(
            this.browser,
            {
                parent: this.qa,
                current: 'activity',
            },
            TestActivity,
        );
        this.activityTypeFilter = new TestActivityTypeFilter(this.browser, {
            parent: this.qa,
            current: 'activityTypeFilter',
        });
    }
}
