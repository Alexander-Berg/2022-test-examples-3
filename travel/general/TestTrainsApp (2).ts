import {assert} from 'chai';
import {index, serp} from 'suites/trains';

import {
    TRAIN_TEST_CONTEXT,
    TRAINS_SUCCESS_TEST_CONTEXT_PARAMS,
} from 'helpers/constants/testContext';

import {ITestFormContacts} from 'components/TestBookingContactsForm/types';
import {ITrainsTestContextTokenParams} from 'helpers/project/trains/api/types/ITrainsTestContextToken';

import {PASSENGER} from 'helpers/project/trains/data/passengers';
import {CONTACTS} from 'helpers/project/trains/data/contacts';
import {TrainsIndexPage} from 'helpers/project/trains/pages/TrainsIndexPage/TrainsIndexPage';
import {TestTrainsOrderConfirmationPage} from 'helpers/project/trains/pages/TestTrainsOrderConfirmationPage';
import {TestTrainsOrderPlacesStepPage} from 'helpers/project/trains/pages/TestTrainsOrderPlacesStepPage/TestTrainsOrderPlacesStepPage';
import {TestTrainsOrderPassengersStepPage} from 'helpers/project/trains/pages/TestTrainsOrderPassengersStepPage';
import {TestTrainsGenericOrderPage} from 'helpers/project/trains/pages/TestTrainsGenericOrderPage/TestTrainsGenericOrderPage';
import {TestTrainsPaymentPage} from 'helpers/project/trains/pages/TestTrainsPaymentPage';
import {TestTrainsDirectionPage} from 'helpers/project/trains/pages/TestTrainsDirectionPage/TestTrainsDirectionPage';
import {TestTrainsApiClient} from 'helpers/project/trains/api/TestTrainsApiClient';
import {TestGenericOrderHappyPage} from 'helpers/project/trains/pages/TestGenericOrderHappyPage/TestGenericOrderHappyPage';
import {TestTrainsGenericSearchPage} from 'helpers/project/trains/pages/TestTrainsGenericSearchPage/TestTrainsGenericSearchPage';
import {TVariantAndSegmentOptions} from 'helpers/project/trains/pages/TestTrainsGenericSearchPage/components/TestTrainsSearchVariant/TestTrainsSearchVariant';
import {PaymentTestContextHelper} from 'helpers/utilities/paymentTestContext/PaymentTestContextHelper';
import TestSingleOrderHappyPage from 'helpers/project/trains/pages/TestSingleOrderHappyPage/TestSingleOrderHappyPage';

import {ITrainsTestFormDocument} from '../components/TestTrainsBookingPassengerForm';

export interface IDataConfirmationStep {
    price: number;
    departureDate: string | undefined;
    departureTime: string;
    arrivalDate: string | undefined;
    arrivalTime: string;
    trainNumber: string;
    coachNumber: string;
    places: string;
}

export class TestTrainsApp {
    indexPage: TrainsIndexPage;
    directionPage: TestTrainsDirectionPage;
    searchPage: TestTrainsGenericSearchPage;

    orderPlacesStepPage: TestTrainsOrderPlacesStepPage;
    orderPassengersStepPage: TestTrainsOrderPassengersStepPage;
    orderConfirmationStepPage: TestTrainsOrderConfirmationPage;
    paymentPage: TestTrainsPaymentPage;
    genericOrderPage: TestTrainsGenericOrderPage;
    happyPage: TestSingleOrderHappyPage;
    genericHappyPage: TestGenericOrderHappyPage;

    paymentTestContextHelper: PaymentTestContextHelper;

    private readonly browser: WebdriverIO.Browser;
    private readonly apiClient: TestTrainsApiClient;

    constructor(browser: WebdriverIO.Browser) {
        this.browser = browser;

        this.indexPage = new TrainsIndexPage(browser);
        this.searchPage = new TestTrainsGenericSearchPage(browser);
        this.directionPage = new TestTrainsDirectionPage(browser);

        this.orderPlacesStepPage = new TestTrainsOrderPlacesStepPage(browser);
        this.orderPassengersStepPage = new TestTrainsOrderPassengersStepPage(
            browser,
        );
        this.orderConfirmationStepPage = new TestTrainsOrderConfirmationPage(
            browser,
        );
        this.paymentPage = new TestTrainsPaymentPage(browser);
        this.genericOrderPage = new TestTrainsGenericOrderPage(
            browser,
            'genericOrderPage',
        );

        this.happyPage = new TestSingleOrderHappyPage(browser);
        this.genericHappyPage = new TestGenericOrderHappyPage(browser);

        this.apiClient = new TestTrainsApiClient();

        this.paymentTestContextHelper = new PaymentTestContextHelper(
            this.browser,
        );
    }

    /**
     * Хелперы для перехода по страницам
     * Ф-ии вида goTo{namePage}Page нужно вызывать после вызова предыдущей страницы
     */

    async goToIndexPage(): Promise<void> {
        await this.browser.url(index.url);
    }

    async goToSearchPage(
        from: string,
        to: string,
        when: string,
        skipPrices?: boolean,
    ): Promise<string> {
        const url = serp.url(from, to, when);

        const resultUrl = await this.browser.url(url);

        if (!skipPrices) {
            await this.searchPage.waitVariantsAndTariffsLoaded();
        }

        return resultUrl;
    }

    async goToOrderPlaces(
        variantAndSegmentOptions?: TVariantAndSegmentOptions,
    ): Promise<void> {
        const variantAndSegment =
            await this.searchPage.variants.findVariantAndSegmentByOptions(
                variantAndSegmentOptions,
            );

        assert.isDefined(
            variantAndSegment,
            'Не найден вариант с сегментом c указанными условиями',
        );

        await variantAndSegment?.variant?.clickToBoyActionButton();
        await this.orderPlacesStepPage.waitTrainDetailsLoaded();
    }

    async setFirstPassengerViaFirstIntent(
        contacts: ITestFormContacts,
    ): Promise<void> {
        const firstPassenger =
            await this.orderPassengersStepPage.passengers.first();

        await firstPassenger.passengerForm.fillWithIntent();
        await this.orderPassengersStepPage.contacts.fill(contacts);
    }

    async setFirstPassengerViaFields(
        passenger: ITrainsTestFormDocument = PASSENGER,
        contacts: ITestFormContacts = CONTACTS,
    ): Promise<void> {
        const {orderPassengersStepPage} = this;

        await orderPassengersStepPage.fillPassengers([passenger]);
        await orderPassengersStepPage.contacts.fill(contacts);
    }

    async getDataFromConfirmationPage(): Promise<IDataConfirmationStep> {
        const {orderConfirmationStepPage} = this;

        const price =
            await orderConfirmationStepPage.orderSummary.totalPrice.price.getValue();
        const segment = await orderConfirmationStepPage.segments.getSegment();
        const {departureDate, departureTime, arrivalDate, arrivalTime, number} =
            await segment.getInfo();

        const placesBlock = await orderConfirmationStepPage.getPlaces();
        const coachNumber = await placesBlock.coachNumber.getText();
        const places = await placesBlock.places.getText();

        return {
            price,
            departureDate,
            departureTime,
            arrivalDate,
            arrivalTime,
            trainNumber: number,
            coachNumber,
            places,
        };
    }

    async setTestContext(
        params: ITrainsTestContextTokenParams = TRAINS_SUCCESS_TEST_CONTEXT_PARAMS,
    ): Promise<void> {
        const {test_context_token: testContextToken} =
            await this.apiClient.getTestContextToken(params);

        await this.browser.setCookie({
            name: TRAIN_TEST_CONTEXT,
            value: testContextToken,
        });
    }

    async setSearchAutoMock(): Promise<void> {
        await this.browser.setCookie({
            name: 'mockImSearchPath',
            value: 'auto',
        });
        await this.browser.setCookie({
            name: 'mockImPath',
            value: 'auto',
        });
    }
}
