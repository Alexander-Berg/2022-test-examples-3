import {Button} from 'components/Button';
import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export class TestHotelsSearchPageMap extends Component {
    toggleButton: Component;
    zoomInButton: Button;
    zoomOutButton: Button;
    currentGeoLocationButton: Button;
    markers: ComponentArray;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.toggleButton = new Component(browser, {
            parent: this.qa,
            current: 'toggleViewButton',
        });
        this.zoomInButton = new Button(browser, {
            parent: this.qa,
            current: 'zoomIn',
        });
        this.zoomOutButton = new Button(browser, {
            parent: this.qa,
            current: 'zoomOut',
        });
        this.currentGeoLocationButton = new Button(browser, {
            parent: this.qa,
            current: 'currentGeoLocation',
        });

        this.markers = new ComponentArray(
            browser,
            {parent: this.qa, current: 'marker'},
            Component,
        );
    }
}
