import {Component} from 'components/Component';

export class TestNavigation extends Component {
    async isActive(): Promise<boolean> {
        const ariaCurrent = await this.getAttribute('aria-current');

        return ariaCurrent === 'page';
    }
}
