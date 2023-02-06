import {Component} from 'components/Component';

export class TestCheckButton extends Component {
    async isChecked(): Promise<boolean> {
        const checked = await this.getAttribute('aria-pressed');

        return checked === 'true';
    }
}
