import {Component} from 'components/Component';

export default class TestTab extends Component {
    async isActive(): Promise<boolean> {
        const active = await this.getAttribute('data-active');

        return active === 'true';
    }
}
