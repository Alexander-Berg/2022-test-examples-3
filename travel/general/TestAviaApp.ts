import {index} from 'suites/avia';
import {stringify} from 'querystring';

import {
    IAviaTestContextParams,
    IAviaTestContextUrlParams,
} from 'helpers/project/avia/api/types/AviaTestContext';
import {ITestFormContacts} from 'components/TestBookingContactsForm/types';

import {TestAviaApiClient} from 'helpers/project/avia/api/TestAviaApiClient';
import {
    AviaSearchResultsDesktopPage,
    AviaSearchResultsPage,
    IAviaSearchParams,
} from 'helpers/project/avia/pages/SearchResultsPage/SearchResultsPage';
import {
    AviaCreateOrderPage,
    TestIndexAviaPage,
    AviaOrderPage,
    AviaPaymentOrderPage,
} from 'helpers/project/avia/pages';
import {IAviaPassenger} from 'helpers/project/avia/pages/CreateOrderPage/passengers';
import {ITestCard} from 'helpers/project/common/TestTrustForm/card';
import {TestAviaHappyPage} from 'helpers/project/avia/pages/TestAviaHappyPage/TestAviaHappyPage';
import {AccountOrderPage} from 'helpers/project/avia/pages/AccountOrderPage/AccountOrderPage';
import {MASTER_CARD} from 'helpers/project/avia/pages/PaymentOrderPage/cards';
import {ITestAviaFlight} from 'helpers/project/avia/components/FlightInfo';
import {TestAviaFlightPage} from 'helpers/project/avia/pages/TestAviaFlightPage/TestAviaFlightPage';
import {AviaDirectionPage} from 'helpers/project/avia/pages/AviaDirectionPage/AviaDirectionPage';

interface ITestContextOptions {
    skipPayment: boolean;
}

const DEFAULT_AVIA_CLASS = 'economy';
const DEFAULT_TRAVELERS_COUNT = {
    adults: 1,
    children: 0,
    infants: 0,
};

export class TestAviaApp {
    private static getSearchUrl(options: IAviaSearchParams): string {
        const {from, to, travellers, startDate, endDate, klass, filters} =
            options;
        const query = {
            fromId: from.id,
            fromName: from.name,
            toId: to.id,
            toName: to.name,
            when: startDate,
            return_date: `${endDate || ''}`,
            oneway: endDate ? 2 : 1,
            klass: klass || DEFAULT_AVIA_CLASS,
            adult_seats: travellers?.adults || DEFAULT_TRAVELERS_COUNT.adults,
            children_seats:
                travellers?.children || DEFAULT_TRAVELERS_COUNT.children,
            infant_seats:
                travellers?.infants || DEFAULT_TRAVELERS_COUNT.infants,
        };

        return `/avia/search/result/?${stringify(query)}${
            filters ? `#${filters}` : ''
        }`;
    }

    indexPage: TestIndexAviaPage;
    searchPage: AviaSearchResultsPage;
    searchDesktopPage: AviaSearchResultsDesktopPage;
    createOrderPage: AviaCreateOrderPage;
    paymentPage: AviaPaymentOrderPage;
    happyPage: TestAviaHappyPage;
    accountOrderPage: AccountOrderPage;
    orderPage: AviaOrderPage;
    flightPage: TestAviaFlightPage;
    directionPage: AviaDirectionPage;

    private readonly browser: WebdriverIO.Browser;
    private readonly testContext?: IAviaTestContextParams;
    private readonly options: ITestContextOptions;

    constructor(
        browser: WebdriverIO.Browser,
        testContext?: IAviaTestContextParams,
        options: ITestContextOptions = {skipPayment: true},
    ) {
        this.browser = browser;
        this.testContext = testContext;
        this.options = options;

        this.indexPage = new TestIndexAviaPage(browser);
        this.searchPage = new AviaSearchResultsPage(browser);
        this.searchDesktopPage = new AviaSearchResultsDesktopPage(browser);
        this.createOrderPage = new AviaCreateOrderPage(browser);
        this.paymentPage = new AviaPaymentOrderPage(browser);
        this.happyPage = new TestAviaHappyPage(browser);
        this.accountOrderPage = new AccountOrderPage(browser);
        this.orderPage = new AviaOrderPage(browser);
        this.flightPage = new TestAviaFlightPage(browser);
        this.directionPage = new AviaDirectionPage(this.browser);
    }

    async goToIndexPage(): Promise<void> {
        await this.browser.url(index.url);
    }

    async goToSearchPage(
        options: IAviaSearchParams,
    ): Promise<AviaSearchResultsPage> {
        await this.setTestContextCookies(await this.getTestContextParams());
        await this.browser.url(TestAviaApp.getSearchUrl(options));
        await this.searchPage.waitForSearchComplete();

        return this.searchPage;
    }

    async book(
        searchParams: IAviaSearchParams,
        {
            passengers,
            contacts,
        }: {
            passengers: IAviaPassenger[];
            contacts: ITestFormContacts;
        },
    ): Promise<{
        price: string;
        flights: ITestAviaFlight[];
    }> {
        const searchPage = await this.goToSearchPage(searchParams);

        await this.moveToBooking(searchPage);

        await this.createOrderPage.waitPageReadyForInteraction();

        const price = await this.createOrderPage.priceInfo.getPrice();
        const flights = await this.createOrderPage.flightInfo.map(flight =>
            flight.getFlightData(),
        );

        await this.createOrderPage.fillBookingForm(passengers, contacts);
        await this.createOrderPage.goToPayment();

        return {
            price,
            flights,
        };
    }

    async pay(card: ITestCard = MASTER_CARD): Promise<void> {
        await this.paymentPage.waitForPageLoading();
        await this.paymentPage.pay(card);
    }

    async moveToBooking(searchPage: AviaSearchResultsPage): Promise<void> {
        const searchVariant = await searchPage.variants.first();

        await searchVariant.moveToOrder();

        const orderPage = new AviaOrderPage(this.browser);
        const companyOffer = await orderPage.offers.company;

        if (!companyOffer) {
            throw new Error(
                'Не найдено предложение для покупки на Яндексе (Аэрофлот)',
            );
        }

        await companyOffer.scrollIntoView();
        await companyOffer.click();

        // явным образом переключаем вкладку, т.к. без этой команды
        // вебдрайвер продолжит взаимодействоватьс неактивной вкладкой
        await this.browser.switchToNextTab();
    }

    private async getTestContextParams(): Promise<
        IAviaTestContextUrlParams | undefined
    > {
        if (!this.testContext) {
            return Promise.resolve(undefined);
        }

        const testContextApi = new TestAviaApiClient();
        const tokens = await testContextApi.getTestContextToken(
            this.testContext,
        );

        const variantTestContext = tokens.token;
        const paymentTestContext = tokens.paymentToken;

        if (this.options.skipPayment) {
            return {
                variantTestContext,
                paymentTestContext,
            };
        }

        return {variantTestContext};
    }

    private async setTestContextCookies(
        context: IAviaTestContextUrlParams | undefined,
    ): Promise<void> {
        if (!context) {
            return;
        }

        if (context.paymentTestContext) {
            await this.browser.setCookies({
                name: 'paymentTestContext',
                value: context.paymentTestContext,
            });
        }

        if (context.variantTestContext) {
            await this.browser.setCookies({
                name: 'variantTestContext',
                value: context.variantTestContext,
            });
        }
    }
}
