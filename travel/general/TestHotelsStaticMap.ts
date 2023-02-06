import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export class TestHotelsStaticMap extends Component {
    readonly activeHotelCard: Component;
    readonly markers: ComponentArray;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.activeHotelCard = new Component(this.browser, {
            parent: this.qa,
            current: 'activeHotelCard',
        });
        this.markers = new ComponentArray(
            this.browser,
            {
                parent: this.qa,
                current: 'marker',
            },
            Component,
        );
    }

    async hasActiveHotelCard(): Promise<boolean> {
        return this.activeHotelCard.isVisible();
    }

    async hasMarkers(): Promise<boolean> {
        return (await this.markers.count()) > 0;
    }
}
