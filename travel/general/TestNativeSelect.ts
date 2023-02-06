import {Component} from 'components/Component';

/** TODO: добавить работу с options, как свойствами TestNativeSelect */
export class TestNativeSelect extends Component {
    async selectByValue(value: string | number): Promise<void> {
        const selector = this.prepareQaSelector(`option_${value}`);

        await this.click();

        const option = await this.browser.$(selector);

        option.click();
    }
}
