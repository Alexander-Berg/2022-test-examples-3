import {Component, ComponentArray} from 'helpers/project/common/components';

export class TestAboutHotel extends Component {
    mainAmenities: ComponentArray;
    toggleAmenities: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.mainAmenities = new ComponentArray(
            browser,
            {
                path: [this.qa],
                current: 'mainAmenity',
            },
            Component,
        );
        this.toggleAmenities = new Component(browser, {
            parent: this.qa,
            current: 'toggleAmenities',
        });
    }
}
