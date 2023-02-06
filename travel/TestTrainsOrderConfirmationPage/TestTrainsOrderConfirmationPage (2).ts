import {URL} from 'url';

import {retry} from 'helpers/project/common/retry';
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

export class TestTrainsOrderConfirmationPage extends TrainsOrderPageLayout {
    readonly segments: TestTrainOrderSegments;
    readonly places: ComponentArray<TestTrainsConfirmStepPlaces>;
    readonly passengers: TestPassengers;
    readonly cancelButton: Button;
    readonly timer: TestConfirmationTimer;
    readonly contacts: TestContacts;
    readonly partnersRequisites: TestPartnersRequisites;
    readonly confirmReadyToInteraction: Component;
    readonly orderButton: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'confirmStep') {
        super(browser, qa);

        this.segments = new TestTrainOrderSegments(browser);
        this.places = new ComponentArray(
            browser,
            'trainsOrderPlaces',
            TestTrainsConfirmStepPlaces,
        );
        this.passengers = new TestPassengers(browser, {
            parent: this.qa,
            current: 'passengers',
        });
        this.timer = new TestConfirmationTimer(browser);
        this.cancelButton = new Button(browser, {
            parent: this.qa,
            current: 'cancelButton',
        });
        this.contacts = new TestContacts(browser, {
            parent: this.qa,
            current: 'contacts',
        });

        this.partnersRequisites = new TestPartnersRequisites(browser, {
            parent: this.qa,
            current: 'partnersRequisites',
        });

        this.confirmReadyToInteraction = new Component(
            browser,
            'confirm-ready-to-interaction',
        );

        this.orderButton = new Button(browser, {
            parent: 'pageOrderSummary',
            current: 'orderButton',
        });
    }

    async goNextStep(): Promise<void> {
        const orderId = await this.extractOrderIdFromUrl();

        if (orderId) {
            await this.browser.setMeta('orderId', orderId);
        }

        await this.orderSummary.orderButton.click();
    }

    async getPlaces(index: number = 0): Promise<TestTrainsConfirmStepPlaces> {
        return (await this.places.items)[index];
    }

    async waitOrderLoaded(): Promise<void> {
        try {
            await retry(
                async () => {
                    await this.confirmReadyToInteraction.waitForVisible(3000);
                },
                {attempts: 25, delay: 1000},
            )();
        } catch (err) {
            throw new Error('Информация о заказе не загрузилась');
        }
    }

    async addInsurance(): Promise<void> {
        const {checkbox} = this.orderSummary.insurance;

        await checkbox.scrollIntoView();
        await checkbox.click();
    }

    async insuranceIsDisplayed(): Promise<boolean> {
        try {
            const {checkbox} = this.orderSummary.insurance;

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
