import {Component} from 'components/Component';

export class TestPlacesAndPrice extends Component {
    placeTypeAndCount: Component;
    price: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.placeTypeAndCount = new Component(browser, {
            parent: this.qa,
            current: 'placeTypeAndCount',
        });

        this.price = new Component(browser, {
            parent: this.qa,
            current: 'price',
        });
    }

    /**
     * Нет мест такого типа
     */
    async noPlaces(): Promise<boolean> {
        const text = await this.placeTypeAndCount.getText();

        return text.includes('нет');
    }
}
