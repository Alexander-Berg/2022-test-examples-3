import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';
import TestActiveTripsListItem from 'components/TestNavigationSideSheet/components/TestActiveTripsList/components/TestActiveTripsListItem';

export default class TestActiveTripsList extends Component {
    readonly activeTripListItems: ComponentArray<TestActiveTripsListItem>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.activeTripListItems = new ComponentArray(
            browser,
            {parent: this.qa, current: 'activeTripsListItem'},
            TestActiveTripsListItem,
        );
    }
}
