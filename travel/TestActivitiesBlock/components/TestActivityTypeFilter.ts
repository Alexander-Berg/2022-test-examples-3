import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';
import {TestCheckButton} from 'components/TestCheckButton';

export default class TestActivityTypeFilter extends Component {
    activityTypes: ComponentArray<TestCheckButton>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.activityTypes = new ComponentArray(
            this.browser,
            {parent: this.qa, current: 'activityType'},
            TestCheckButton,
        );
    }

    getActive(): Promise<TestCheckButton | undefined> {
        return this.activityTypes.find(typeButton => typeButton.isChecked());
    }
}
