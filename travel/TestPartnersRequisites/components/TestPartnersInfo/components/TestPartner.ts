import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';
import TestPartnerField from 'components/TestPartnersRequisites/components/TestPartnersInfo/components/TestPartnerField';

export default class TestPartner extends Component {
    readonly title: Component;
    readonly partnerFields: ComponentArray<TestPartnerField>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });

        this.partnerFields = new ComponentArray(
            browser,
            {parent: this.qa, current: 'partnerField'},
            TestPartnerField,
        );
    }

    async getPartnerTitle(): Promise<string> {
        return await this.title.getText();
    }

    async checkAllFieldsFilled(): Promise<boolean> {
        const partnerFields = this.partnerFields;

        return await partnerFields.every(async field => {
            return (
                (await field.value.getText()).length > 0 &&
                (await field.name.getText()).length > 0
            );
        });
    }
}
