import {Component} from 'components/Component';

export class TestCheckbox extends Component {
    async isChecked(): Promise<boolean> {
        const checked = await this.getAttribute('aria-checked');

        return checked === 'true';
    }

    async uncheck(): Promise<void> {
        /**
         * Castyl: В попапах чекбокс нажимается не с первого раза =(
         */
        while (await this.isChecked()) {
            await this.click();
        }
    }
}
