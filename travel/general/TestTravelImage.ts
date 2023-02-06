import {Component} from 'components/Component';

export default class TestTravelImage extends Component {
    getSrc(): Promise<string | null> {
        return this.getAttribute('src');
    }
}
