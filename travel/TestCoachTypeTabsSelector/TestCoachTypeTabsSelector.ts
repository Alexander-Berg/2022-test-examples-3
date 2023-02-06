import {ECoachType} from 'helpers/project/trains/types/coachType';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';
import {TestCoachTabButton} from './components/TestCoachTabButton';

export class TestCoachTypeTabsSelector extends Component {
    tabButtons: ComponentArray<TestCoachTabButton>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.tabButtons = new ComponentArray(
            browser,
            {
                parent: this.qa,
                current: 'tabButton',
            },
            TestCoachTabButton,
        );
    }

    async getActiveTabButton(): Promise<Component | undefined> {
        return await this.tabButtons.find(async tabButton => {
            return (await tabButton.getAttribute('data-active')) === 'true';
        });
    }

    async setActiveCoachType(coachType: ECoachType): Promise<void> {
        const tabButton = await this.getTabButtonByType(coachType);

        if (!tabButton) {
            throw new Error(`Не найден таб с типом: ${coachType}`);
        }

        await tabButton.setActive();
    }

    async getTabButtonByType(
        coachType: ECoachType,
    ): Promise<TestCoachTabButton | undefined> {
        return this.tabButtons.find(tabButton =>
            tabButton.qa.includes(coachType),
        );
    }
}
