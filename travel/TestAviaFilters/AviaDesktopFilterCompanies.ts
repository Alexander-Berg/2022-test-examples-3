import {AviaFilterCompaniesCheckbox} from 'helpers/project/avia/pages/SearchResultsPage/components/TestAviaFilters/components/AviaFilterCompaniesCheckbox';

import {Radio} from 'components/Radio';
import {TestCheckbox} from 'components/TestCheckbox';
import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export class AviaDesktopFilterCompanies extends Component {
    readonly allAlliences: Radio;
    readonly allience: Radio;
    readonly selectAllCompanies: TestCheckbox;
    readonly selectCompany: ComponentArray<AviaFilterCompaniesCheckbox>;
    readonly combinationAviaCompanies: TestCheckbox;

    constructor(
        browser: WebdriverIO.Browser,
        qa: QA = 'avia-desktop-filters-companies',
    ) {
        super(browser, qa);

        // Когда потребуется проверить взаимодействие с фильтрами -
        // вынести их в самостоятельные компоненты

        this.allAlliences = new Radio(browser, {
            parent: this.qa,
            current: 'allienceSelect-allAlliences',
        });
        this.allience = new Radio(browser, {
            parent: this.qa,
            current: 'allienceSelect-allience',
        });
        this.selectAllCompanies = new TestCheckbox(browser, {
            parent: this.qa,
            current: 'selectAllCompanies',
        });
        this.combinationAviaCompanies = new TestCheckbox(browser, {
            parent: this.qa,
            current: 'combinationAviaCompanies',
        });
        this.selectCompany = new ComponentArray(
            browser,
            {
                parent: this.qa,
                current: 'selectCompany',
            },
            AviaFilterCompaniesCheckbox,
        );
    }
}
