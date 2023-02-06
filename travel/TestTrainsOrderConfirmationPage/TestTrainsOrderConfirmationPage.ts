import {URL} from 'url';

import {MINUTE} from 'helpers/constants/dates';

import TrainsOrderPageLayout from 'helpers/project/trains/components/TrainsOrderPageLayout';
import {TestTrainOrderSegments} from 'helpers/project/trains/components/TestTrainOrderSegments/TestTrainOrderSegments';
import {TestPassengers} from 'helpers/project/trains/pages/TestTrainsOrderConfirmationPage/components/TestPassengers/TestPassengers';
import TestConfirmationTimer from 'helpers/project/trains/pages/TestTrainsOrderConfirmationPage/components/TestConfirmationTimer';

import {ComponentArray} from 'components/ComponentArray';
import {Button} from 'components/Button';
import {TestTrainsConfirmStepPlaces} from './components/TestTrainsConfirmStepPlaces';
import TestContacts from './components/TestContacts';
import {Component} from 'components/Component';
import TestPartnersRequisites from 'components/TestPartnersRequisites/TestPartnersRequisites';

export class TestTrainsOrderConfirmationPage extends Component {
    readonly segments: TestTrainOrderSegments;
    readonly places: ComponentArray<TestTrainsConfirmStepPlaces>;
    readonly passengers: TestPassengers;
    readonly cancelButton: Button;
    readonly timer: TestConfirmationTimer;
    readonly contacts: TestContacts;
    readonly partnersRequisites: TestPartnersRequisites;
    readonly confirmReadyToInteraction: Component;
    readonly orderButton: Button;
    readonly layout: TrainsOrderPageLayout;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'confirmStep') {
        super(browser, qa);

        this.layout = new TrainsOrderPageLayout(this.browser);
        this.segments = new TestTrainOrderSegments(this.browser);
        this.places = new ComponentArray(
            this.browser,
            'trainsOrderPlaces',
            TestTrainsConfirmStepPlaces,
        );
        this.passengers = new TestPassengers(this.browser, {
            parent: this.qa,
            current: 'passengers',
        });
        this.timer = new TestConfirmationTimer(this.browser);
        this.cancelButton = new Button(this.browser, {
            parent: this.qa,
            current: 'cancelButton',
        });
        this.contacts = new TestContacts(this.browser, {
            parent: this.qa,
            current: 'contacts',
        });

        this.partnersRequisites = new TestPartnersRequisites(this.browser, {
            parent: this.qa,
            current: 'partnersRequisites',
        });

        this.confirmReadyToInteraction = new Component(
            this.browser,
            'confirm-ready-to-interaction',
        );

        this.orderButton = new Button(this.browser, {
            parent: 'pageOrderSummary',
            current: 'orderButton',
        });
    }

    async goNextStep(): Promise<void> {
        const orderId = await this.extractOrderIdFromUrl();

        if (orderId) {
            await this.browser.setMeta('orderId', orderId);
        }

        await this.layout.orderSummary.orderButton.scrollIntoView();
        await this.layout.orderSummary.orderButton.click();
    }

    async getPlaces(index: number = 0): Promise<TestTrainsConfirmStepPlaces> {
        return (await this.places.items)[index];
    }

    async waitOrderLoaded(): Promise<void> {
        try {
            await this.confirmReadyToInteraction.waitForExist(MINUTE);
        } catch (err) {
            throw new Error('Информация о заказе не загрузилась');
        }
    }

    async addInsurance(): Promise<void> {
        const {checkbox} = this.layout.orderSummary.insurance;

        await checkbox.scrollIntoView();
        await checkbox.click();
    }

    async insuranceIsDisplayed(): Promise<boolean> {
        try {
            const {checkbox} = this.layout.orderSummary.insurance;

            await checkbox.scrollIntoView();

            return checkbox.isVisible();
        } catch (e) {
            return false;
        }
    }

    private async extractOrderIdFromUrl(): Promise<string | null> {
        const urlStr = await this.browser.getUrl();
        const url = new URL(urlStr);

        return url.searchParams.get('id');
    }
}
