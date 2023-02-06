import {stringify} from 'querystring';
import {hotel} from 'suites/hotels';

import {HOTELS_QA_PREFIX, HOTEL_PAGE_QA, HOTEL_CARD_QA_PATH} from './constants';
import {SECOND} from 'helpers/constants/dates';

import {PageParams} from './types';

import {Component, ComponentArray} from 'helpers/project/common/components';
import {HotelsHeaderSearchInformation} from 'helpers/project/hotels/components/HotelsHeaderSearchInformation';
import {AdFoxBanner} from 'helpers/project/common/components/AdFoxBanner';
import TestOffersInfo from 'helpers/project/hotels/pages/HotelPage/components/TestOffersInfo/TestOffersInfo';

import {HotelsSearchForm} from '../../components/HotelsSearchForm';
import {TestHotelPageState} from './components/TestHotelPageState';
import {TestHotelTransportAccessibility} from './components/TestHotelTransportAccessibility';
import {TestAboutHotel} from './components/TestAboutHotel';
import {TestHotelPageGeoInfo} from './components/TestHotelPageGeoInfo';
import {TestSimilarHotel} from './components/TestSimilarHotel';
import {TestHotelReviews} from './components/TestHotelReviews';

export class TestHotelPage extends Component {
    headerSearch: HotelsHeaderSearchInformation;
    state: TestHotelPageState;
    hotelName: Component;
    hotelAddress: Component;
    rating: Component;
    positiveReview: Component;
    reviewsLink: Component;
    direct: Component;
    adFox: AdFoxBanner;
    mirCashbackBanner: Component;
    backButton: Component;
    gallery: Component;
    aboutHotel: TestAboutHotel;
    geoInfo: TestHotelPageGeoInfo;
    geoFeature: Component;
    transportAccessibility: TestHotelTransportAccessibility;
    addToFavorite: Component;
    similarHotels: ComponentArray<TestSimilarHotel>;
    hotelFeatures: Component;
    hotelReviews: TestHotelReviews;
    breadcrumbs: ComponentArray;
    offersInfo: TestOffersInfo;
    searchForm: HotelsSearchForm;

    constructor(browser: WebdriverIO.Browser) {
        super(browser);

        this.headerSearch = new HotelsHeaderSearchInformation(
            browser,
            HOTELS_QA_PREFIX,
        );
        this.searchForm = new HotelsSearchForm(this.browser);
        this.state = new TestHotelPageState(browser, HOTEL_PAGE_QA);

        this.hotelName = new Component(browser, {
            path: HOTEL_CARD_QA_PATH,
            current: 'hotelName',
        });
        this.hotelAddress = new Component(browser, {
            path: HOTEL_CARD_QA_PATH,
            current: 'hotelAddress',
        });
        this.rating = new Component(browser, {
            path: HOTEL_CARD_QA_PATH,
            current: 'rating',
        });
        this.positiveReview = new Component(browser, {
            path: HOTEL_CARD_QA_PATH,
            current: 'positiveReview',
        });
        this.reviewsLink = new Component(browser, {
            path: HOTEL_CARD_QA_PATH,
            current: 'reviewsLink',
        });

        this.direct = new Component(browser, {
            parent: HOTEL_PAGE_QA,
            current: 'direct',
        });
        this.adFox = new AdFoxBanner(this.browser);
        this.mirCashbackBanner = new Component(browser, {
            parent: HOTEL_PAGE_QA,
            current: 'mirCashbackBanner',
        });
        this.backButton = new Component(browser, {
            parent: HOTEL_PAGE_QA,
            current: 'backButton',
        });
        this.geoFeature = new Component(browser, {
            parent: HOTEL_PAGE_QA,
            current: 'geoFeature',
        });
        this.transportAccessibility = new TestHotelTransportAccessibility(
            browser,
            {
                parent: HOTEL_PAGE_QA,
                current: 'transportAccessibility',
            },
        );
        this.addToFavorite = new Component(browser, 'addToFavorite');
        this.gallery = new Component(browser, 'hotelPageGallery');
        this.aboutHotel = new TestAboutHotel(browser, 'aboutHotel');
        this.geoInfo = new TestHotelPageGeoInfo(browser, 'hotelPageGeoInfo');
        this.offersInfo = new TestOffersInfo(browser, 'hotelOffersInfo');
        this.similarHotels = new ComponentArray(
            browser,
            'similarHotel',
            TestSimilarHotel,
        );
        this.hotelFeatures = new Component(browser, 'hotelFeatures');
        this.hotelReviews = new TestHotelReviews(browser, 'hotelReviews');
        this.breadcrumbs = new ComponentArray(
            browser,
            'hotelPageBreadcrumb',
            Component,
        );
    }

    async openSearchForm(): Promise<void> {
        await this.headerSearch.openSearchForm();

        /**
         * Анимация открытия формы
         */
        await this.browser.pause(SECOND);
    }

    goToHotel(queryParams: PageParams): Promise<string> {
        return this.browser.url(`${hotel.url}?${stringify(queryParams)}`);
    }
}
