import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';
import TestBottomSheet from 'components/TestBottomSheet';

export default class TestActionsDialog extends Component {
    actionItems: ComponentArray;
    bottomSheet: TestBottomSheet;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.bottomSheet = new TestBottomSheet(browser);
        this.actionItems = new ComponentArray(
            this.browser,
            {
                parent: this.qa,
                current: 'actionItem',
            },
            Component,
        );
    }

    async findItem(itemKey: string): Promise<Component> {
        const item = await this.actionItems.find(i => i.qa.startsWith(itemKey));

        if (!item) {
            throw new Error(`Item with key ${itemKey} not found`);
        }

        return item;
    }
}
