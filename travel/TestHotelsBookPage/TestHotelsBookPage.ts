import {stringify} from 'querystring';
import {book} from 'suites/hotels';
import {exponentialDelay} from 'axios-retry';

import {MINUTE} from 'helpers/constants/dates';

import {IBookOfferRequestParams} from 'helpers/project/hotels/types/IBookOffer';
import {ITestBookOfferTokenResponse} from 'helpers/project/hotels/pages/TestHotelsBookPage/components/TestHotelsBookPageApiClient/types';

import {Page} from 'helpers/project/common/components/Page';
import {Loader} from 'helpers/project/common/components/Loader';
import {TestHotelsCancellationInfo} from 'helpers/project/hotels/components/TestHotelsCancellationInfo/TestHotelsCancellationInfo';
import {delay} from 'helpers/project/common/delay';

import {Component} from 'components/Component';
import {TestPriceErrorModal} from 'components/TestPriceErrorModal';
import {TestHotelsBookPageApiClient} from './components/TestHotelsBookPageApiClient/TestHotelsBookPageApiClient';
import {TestHotelsBookPageStatusProvider} from './components/TestHotelsBookPageStatusProvider/TestHotelsBookPageStatusProvider';
import {TestHotelsBookSearchParams} from './components/TestHotelsBookSearchParams/TestHotelsBookSearchParams';
import {TestHotelsBookForm} from './components/TestHotelsBookForm/TestHotelsBookForm';
import {TestHotelsBookPriceInfo} from './components/TestHotelsBookPriceInfo/TestHotelsBookPriceInfo';

/* Constants */
const {url} = book;
const HOTELS_BOOK_PAGE_QA = 'hotelsBookPage';
const BOOK_FORM_QA = 'book-form';
const PRICE_INFO_QA = 'priceInfo';

export class TestHotelsBookPage extends Page {
    readonly bookStatusProvider: TestHotelsBookPageStatusProvider;

    readonly bookSearchParams: TestHotelsBookSearchParams;

    readonly bookSupportPhone: Component;

    readonly hotelName: Component;

    readonly offerName: Component;

    readonly cancellationInfo: TestHotelsCancellationInfo;

    readonly bookForm: TestHotelsBookForm;

    readonly loaderCreateOrder: Loader;

    readonly priceInfo: TestHotelsBookPriceInfo;

    readonly reservedWithRestrictionsModal: TestPriceErrorModal;

    private readonly apiClient: TestHotelsBookPageApiClient;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, HOTELS_BOOK_PAGE_QA);

        this.apiClient = new TestHotelsBookPageApiClient();
        this.bookStatusProvider = new TestHotelsBookPageStatusProvider(
            browser,
            HOTELS_BOOK_PAGE_QA,
        );
        this.bookSearchParams = new TestHotelsBookSearchParams(
            browser,
            HOTELS_BOOK_PAGE_QA,
        );
        this.bookSupportPhone = new Component(browser, {
            parent: this.qa,
            current: 'support-phone',
        });
        this.hotelName = new Component(browser, {
            parent: this.qa,
            current: 'hotel-name',
        });
        this.offerName = new Component(browser, {
            parent: this.qa,
            current: 'offer-name',
        });
        this.cancellationInfo = new TestHotelsCancellationInfo(browser, {
            parent: this.qa,
            current: 'cancellationInfo',
        });
        this.priceInfo = new TestHotelsBookPriceInfo(browser, {
            parent: HOTELS_BOOK_PAGE_QA,
            current: PRICE_INFO_QA,
        });
        this.bookForm = new TestHotelsBookForm(
            browser,
            {parent: HOTELS_BOOK_PAGE_QA, current: BOOK_FORM_QA},
            {parent: this.priceInfo.qa, current: 'submit'},
            {parent: HOTELS_BOOK_PAGE_QA, current: 'goToForm'},
        );
        this.loaderCreateOrder = new Loader(browser, {
            parent: this.qa,
            current: 'createOrderLoader',
        });
        this.reservedWithRestrictionsModal = new TestPriceErrorModal(
            browser,
            'reservedWithRestrictionsModal',
        );
    }

    async goToPage(testOfferParams: IBookOfferRequestParams): Promise<void> {
        try {
            const {offerTokens} = await this.getTestOfferData(testOfferParams);
            const [{token}] = offerTokens;

            await this.browser.url(
                `${url}?${stringify({
                    token: token,
                })}`,
            );
        } catch (e) {
            console.error(
                `???????????????????? ???????????? offerToken. ??????????????????: ${JSON.stringify(
                    testOfferParams,
                )}`,
            );

            throw e;
        }
    }

    async getTestOfferData(
        testOfferParams: IBookOfferRequestParams,
    ): Promise<ITestBookOfferTokenResponse['data']> {
        try {
            for (let i = 0; i < 5; i++) {
                const {data} = await this.apiClient.getTestContextToken(
                    testOfferParams,
                );

                if (data.offerTokens.length) {
                    return data;
                }

                await delay(exponentialDelay(i));
            }

            throw new Error('data.offerTokens is empty');
        } catch (e) {
            console.error(e);

            throw new Error(
                `???? ?????????????? ???????????????? offerToken. ??????????????????: ${JSON.stringify(
                    testOfferParams,
                )}`,
            );
        }
    }

    async waitUntilLoaded(): Promise<void> {
        await this.loaderCreateOrder.waitUntilLoaded(2 * MINUTE);
    }
}
