import moment, {Moment} from 'moment';

import {ITestHotelsSearchParams} from 'helpers/project/hotels/types/ITestHotelsSearchParams';

import humanizePeriod from '../../../../utilities/date/humanizePeriod';
import pluralAdults from '../../../../utilities/plural/pluralAdults';
import pluralChildren from '../../../../utilities/plural/pluralChildren';
import pluralGuests from '../../../../utilities/plural/pluralGuests';
import dateFormats from '../../../../utilities/date/formats';
import parseDatesPeriod from 'helpers/utilities/date/parseDatesPeriod';

import {HotelsSearchForm} from '../../components/HotelsSearchForm';
import {HotelsHeaderSearchInformation} from '../../components/HotelsHeaderSearchInformation';
import {TestHotelsSearchInformation} from './components/TestHotelsSearchInformation';
import {TestHotelsSearchPageFiltersBar} from './components/TestHotelsSearchPageFiltersBar';
import {TestHotelsSearchResults} from './components/TestHotelsSearchResults';
import {TestHotelsSearchPageMap} from './components/TestHotelsSearchPageMap';
import {TestHotelsMapListRadioButton} from './components/TestHotelsMapListRadioButton';
import {Page} from 'components/Page';

const HOTELS_QA_PREFIX = 'hotels';
const SEARCH_PAGE_QA_PREFIX = `${HOTELS_QA_PREFIX}-searchPage`;

interface ISearchInformation {
    date: string;
    direction?: string;
}

interface ISearchFormInformation {
    checkinDate: string;
    checkoutDate: string;
    guests: string;
    direction?: string | null;
}

export class TestHotelsSearchPage extends Page {
    searchForm: HotelsSearchForm;
    searchInformation: TestHotelsSearchInformation;
    hotelsHeaderSearchInformation: HotelsHeaderSearchInformation;
    filtersBar: TestHotelsSearchPageFiltersBar;
    searchResults: TestHotelsSearchResults;
    map: TestHotelsSearchPageMap;
    mapListRadioButtons: TestHotelsMapListRadioButton;

    constructor(browser: WebdriverIO.Browser) {
        super(browser);

        this.searchForm = new HotelsSearchForm(this.browser);
        this.searchInformation = new TestHotelsSearchInformation(this.browser, {
            parent: SEARCH_PAGE_QA_PREFIX,
            current: 'searchInformation',
        });

        this.hotelsHeaderSearchInformation = new HotelsHeaderSearchInformation(
            this.browser,
            HOTELS_QA_PREFIX,
        );
        this.filtersBar = new TestHotelsSearchPageFiltersBar(this.browser, {
            parent: SEARCH_PAGE_QA_PREFIX,
            current: 'filtersBar',
        });
        this.searchResults = new TestHotelsSearchResults(
            this.browser,
            SEARCH_PAGE_QA_PREFIX,
        );
        this.map = new TestHotelsSearchPageMap(this.browser, {
            parent: SEARCH_PAGE_QA_PREFIX,
            current: 'map',
        });
        this.mapListRadioButtons = new TestHotelsMapListRadioButton(
            this.browser,
            {
                parent: SEARCH_PAGE_QA_PREFIX,
                current: 'mapListRadioButton',
            },
        );
    }

    /* SearchForm actions */

    async openSearchForm(): Promise<void> {
        await this.hotelsHeaderSearchInformation.openSearchForm();
    }

    async getSearchFormDates(): Promise<{startDate: Moment; endDate: Moment}> {
        if (this.isDesktop) {
            return this.searchForm.getDates();
        }

        const dates =
            await this.hotelsHeaderSearchInformation.searchInformation.date.getText();

        return parseDatesPeriod(dates);
    }

    /* SearchParams providers */

    async getHeaderSearchInformation(): Promise<ISearchInformation> {
        const {date, direction} =
            await this.hotelsHeaderSearchInformation.searchInformation.getSearchParams();

        return {
            date,
            direction,
        };
    }

    getSearchInformationSectionTitleByParams(
        searchParams: ITestHotelsSearchParams,
    ): string {
        const {checkinDate, checkoutDate, place, adults, childrenAges} =
            searchParams;
        const datePeriod = humanizePeriod(checkinDate, checkoutDate);
        const childrenLabel = pluralChildren(childrenAges);

        return `${place}, ${datePeriod}, ${pluralAdults(adults)}${
            childrenLabel ? `, ${childrenLabel}` : ''
        }`;
    }

    getHeaderSearchInformationByParams(
        searchParams: ITestHotelsSearchParams,
    ): ISearchInformation {
        const {checkinDate, checkoutDate, place} = searchParams;
        const date = humanizePeriod(checkinDate, checkoutDate, true);

        return {
            date,
            direction: place,
        };
    }

    async getSearchFormInformation(): Promise<ISearchFormInformation> {
        const checkinDate =
            await this.searchForm.period.startTrigger.value.getText();
        const checkoutDate =
            await this.searchForm.period.endTrigger.value.getText();
        const guests = await this.searchForm.travellers.trigger.getText();
        const direction = await this.searchForm.place.getInputValue();

        return {
            direction,
            checkinDate,
            checkoutDate,
            guests,
        };
    }

    getSearchFormInformationByParams(
        searchParams: ITestHotelsSearchParams,
    ): ISearchFormInformation {
        const {checkinDate, checkoutDate, place, adults, childrenAges} =
            searchParams;
        const guests =
            childrenAges && childrenAges.length
                ? pluralGuests(adults + childrenAges.length)
                : pluralAdults(adults);

        return {
            checkinDate: moment(checkinDate).format(dateFormats.HUMAN_SHORT),
            checkoutDate: moment(checkoutDate).format(dateFormats.HUMAN_SHORT),
            guests,
            direction: place,
        };
    }
}
