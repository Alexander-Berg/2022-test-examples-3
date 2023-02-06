import {SECOND} from 'helpers/constants/dates';

import TestActiveTripItem from 'helpers/project/account/pages/TripsPage/components/TestActiveTripItem';
import TestPastTrips from 'helpers/project/account/pages/TripsPage/components/TestPastTrips';
import TestEmptyTripsPlaceholder from 'helpers/project/account/pages/TripsPage/components/TestEmptyTripsPlaceholder';
import TestNoAuthTripsPage from 'helpers/project/account/pages/TripsPage/components/TestNoAuthTripsPage';

import {Component} from 'components/Component';
import {Button} from 'components/Button';
import {ComponentArray} from 'components/ComponentArray';
import {Loader} from 'components/Loader';

export default class TestTripsPageContent extends Component {
    readonly title: Component;
    readonly searchOrderLink: Component;
    readonly moreTripsButton: Button;
    readonly pastTripsTitle: Component;
    readonly activeTrips: ComponentArray<TestActiveTripItem>;
    readonly pastTrips: TestPastTrips;
    readonly emptyTripsPlaceholder: TestEmptyTripsPlaceholder;
    readonly loader: Loader;
    readonly noAuthTripsPage: TestNoAuthTripsPage;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });

        this.searchOrderLink = new Component(browser, {
            parent: this.qa,
            current: 'searchOrderLink',
        });

        this.pastTripsTitle = new Component(browser, {
            parent: this.qa,
            current: 'pastTripsTitle',
        });

        this.moreTripsButton = new Button(browser, {
            parent: this.qa,
            current: 'moreTripsButton',
        });

        this.activeTrips = new ComponentArray(
            browser,
            {parent: this.qa, current: 'activeTrip'},
            TestActiveTripItem,
        );

        this.pastTrips = new TestPastTrips(browser, {
            parent: this.qa,
            current: 'pastTrips',
        });

        this.emptyTripsPlaceholder = new TestEmptyTripsPlaceholder(browser, {
            parent: this.qa,
            current: 'emptyTripsPlaceholder',
        });

        this.loader = new Loader(browser, {parent: this.qa, current: 'loader'});

        this.noAuthTripsPage = new TestNoAuthTripsPage(browser, {
            parent: this.qa,
            current: 'noAuthTripsPage',
        });
    }

    /**
     * Пока кнопка подгрузки заблокирована, поездки подгружаются
     */
    async waitUntilMoreTripsLoaded(): Promise<void> {
        await this.browser.waitUntil(
            async () => {
                const moreButtonIsDisabled =
                    (await this.moreTripsButton.isDisplayed()) &&
                    (await this.moreTripsButton.isDisabled());

                return !moreButtonIsDisabled;
            },
            {timeout: 5 * SECOND},
        );
    }
}
