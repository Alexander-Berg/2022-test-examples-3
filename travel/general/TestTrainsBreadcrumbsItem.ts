import {Component} from 'components/Component';

export class TestTrainsBreadcrumbsItem extends Component {
    async getUrl() {
        return this.getAttribute('href');
    }
}
