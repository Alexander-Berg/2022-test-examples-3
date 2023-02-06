import {Component} from 'components/Component';
import {Button} from 'components/Button';
import {ComponentArray} from 'components/ComponentArray';

import {TestPlacesAndPrice} from './TestPlacesAndPrice';
import {TestTrainsPlacesViewType} from './TestTrainsPlacesViewType';

export class TestCoachTypeGroupItem extends Component {
    classTitle: Component;
    nextStepButton: Button;
    placesViewType: TestTrainsPlacesViewType;
    facilitiesAndAbilities: Component;
    description: Component;
    company: Component;
    places: ComponentArray<TestPlacesAndPrice>;
    autoSeatDescription: Component;
    selectClassButton: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.classTitle = new Component(browser, {
            parent: this.qa,
            current: 'classTitle',
        });

        this.nextStepButton = new Button(browser, {
            parent: this.qa,
            current: 'nextStepButton',
        });

        this.placesViewType = new TestTrainsPlacesViewType(browser, {
            parent: this.qa,
            current: 'placesViewType',
        });

        this.facilitiesAndAbilities = new Component(browser, {
            parent: this.qa,
            current: 'facilitiesAndAbilities',
        });

        this.description = new Component(browser, {
            parent: this.qa,
            current: 'description',
        });

        this.autoSeatDescription = new Component(browser, {
            parent: this.qa,
            current: 'autoSeatDescription',
        });

        this.company = new Component(browser, {
            parent: this.qa,
            current: 'company',
        });

        this.places = new ComponentArray(
            browser,
            {
                parent: this.qa,
                current: 'placeAndPrice',
            },
            TestPlacesAndPrice,
        );

        this.selectClassButton = new Button(browser, {
            parent: this.qa,
            current: 'selectClassButton',
        });
    }

    async isExpanded(): Promise<boolean> {
        return this.placesViewType.isDisplayed();
    }

    async hasPlacesWithPrice(): Promise<boolean> {
        return this.places.every(async place => {
            if (await place.isDisplayed()) {
                return true;
            }

            const text = await place.getText();

            if (text.includes('мест нет')) {
                return true;
            }

            return Boolean(
                (await place.placeTypeAndCount.getText()) &&
                    (await place.price.getText()),
            );
        });
    }

    async getServiceClassCode(): Promise<string | undefined> {
        const classTitle = await this.classTitle.getText();

        return classTitle.match(/\s+(\d[А-ЯЁ])/)?.[1];
    }
}
