import {AviaDesktopFilterCompanies} from 'helpers/project/avia/pages/SearchResultsPage/components/TestAviaFilters/AviaDesktopFilterCompanies';

import {Component} from 'components/Component';

export class AviaDesktopFilters extends Component {
    readonly sorting: Component;
    readonly noTransfers: Component;
    readonly baggage: Component;
    readonly transfers: Component;
    readonly time: Component;
    readonly airports: Component;
    readonly companies: AviaDesktopFilterCompanies;
    readonly partners: Component;
    readonly reset: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'avia-desktop-filters') {
        super(browser, qa);

        // Когда потребуется проверить взаимодействие с фильтрами -
        // вынести их в самостоятельные компоненты
        this.sorting = new Component(browser, {
            parent: this.qa,
            current: 'sorting',
        });
        this.noTransfers = new Component(browser, {
            parent: this.qa,
            current: 'no-transfer',
        });
        this.baggage = new Component(browser, {
            parent: this.qa,
            current: 'baggage',
        });
        this.transfers = new Component(browser, {
            parent: this.qa,
            current: 'transfers',
        });
        this.time = new Component(browser, {parent: this.qa, current: 'time'});
        this.airports = new Component(browser, {
            parent: this.qa,
            current: 'airports',
        });
        this.companies = new AviaDesktopFilterCompanies(browser, {
            parent: this.qa,
            current: 'companies',
        });
        this.partners = new Component(browser, {
            parent: this.qa,
            current: 'partners',
        });
        this.reset = new Component(browser, {
            parent: this.qa,
            current: 'reset',
        });
    }
}
