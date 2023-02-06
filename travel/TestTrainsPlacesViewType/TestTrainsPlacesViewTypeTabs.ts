import {filter, find} from 'p-iteration';

import {Button, Component} from '../../../common/components';

export class TestTrainsPlacesViewTypeTabs extends Component {
    schemasTab: Button;
    requirementsTab: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.schemasTab = new Button(browser, {
            key: 'schemas',
            parent: this.qa,
            current: 'tab',
        });

        this.requirementsTab = new Button(browser, {
            key: 'requirements',
            parent: this.qa,
            current: 'tab',
        });
    }

    get visibleTabs() {
        return filter(
            [this.schemasTab, this.requirementsTab],
            async tab => await tab.isDisplayed(),
        );
    }

    async getActiveTab() {
        return await find(await this.visibleTabs, async tab =>
            Boolean(await tab.getAttribute('data-active')),
        );
    }
}
