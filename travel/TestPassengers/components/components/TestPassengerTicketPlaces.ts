import {Component} from 'components/Component';

export default class TestPassengerTicketPlaces extends Component {
    placeNumbers: Component;
    placesType: Component;
    coachGender: Component;
    noPlaceBaby: Component;
    noPlace: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.placeNumbers = new Component(browser, {
            parent: this.qa,
            current: 'placeNumbers',
        });

        this.placesType = new Component(browser, {
            parent: this.qa,
            current: 'placesType',
        });

        this.coachGender = new Component(browser, {
            parent: this.qa,
            current: 'coachGender',
        });

        this.noPlaceBaby = new Component(browser, {
            parent: this.qa,
            current: 'noPlaceBaby',
        });

        this.noPlace = new Component(browser, {
            parent: this.qa,
            current: 'noPlace',
        });
    }
}
