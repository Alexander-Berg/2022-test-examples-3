import {SECOND} from 'helpers/constants/dates';

import {Button} from 'components/Button';
import {TestUser} from 'components/TestUser';
import {Component} from 'components/Component';
import TestActiveTripsList from 'components/TestNavigationSideSheet/components/TestActiveTripsList/TestActiveTripsList';
import {TestLink} from 'components/TestLink';

export class TestNavigationSideSheet extends Component {
    readonly toggleButton: Button;
    readonly user: TestUser;
    readonly tripsLink: TestLink;
    readonly activeTripsList: TestActiveTripsList;

    readonly aviaLink: TestLink;
    readonly trainsLink: TestLink;
    readonly hotelsLink: TestLink;
    readonly busesLink: TestLink;

    constructor(browser: WebdriverIO.Browser, qa?: QA) {
        super(browser, qa);

        this.toggleButton = new Button(browser, {
            parent: this.qa,
            current: 'toggleButton',
        });

        this.user = new TestUser(browser, {parent: this.qa, current: 'user'});

        this.tripsLink = new TestLink(browser, {
            parent: this.qa,
            current: 'tripsLink',
        });

        this.activeTripsList = new TestActiveTripsList(browser, {
            parent: this.qa,
            current: 'activeTripsList',
        });

        this.aviaLink = this.getProjectLink('avia');
        this.trainsLink = this.getProjectLink('trains');
        this.hotelsLink = this.getProjectLink('hotels');
        this.busesLink = this.getProjectLink('buses');
    }

    async close(): Promise<void> {
        await this.browser.back();

        // Ждем анимацию шторки
        await this.browser.pause(SECOND);
    }

    private getProjectLink(project: string): TestLink {
        return new TestLink(this.browser, {
            parent: this.qa,
            current: 'projectLink',
            key: project,
        });
    }
}
