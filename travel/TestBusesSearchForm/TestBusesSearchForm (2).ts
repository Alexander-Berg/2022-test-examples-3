import {Component} from 'components/Component';
import {SearchSuggest} from 'components/SearchSuggest';
import {Button} from 'components/Button';

export default class TestBusesSearchForm extends Component {
    fromSuggest: SearchSuggest;
    toSuggest: SearchSuggest;
    submitButton: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'buses-search-form') {
        super(browser, qa);

        this.fromSuggest = new SearchSuggest(this.browser, {
            parent: this.qa,
            current: 'search-suggest-from',
        });
        this.toSuggest = new SearchSuggest(this.browser, {
            parent: this.qa,
            current: 'search-suggest-to',
        });
        this.submitButton = new Button(this.browser, {
            parent: this.qa,
            current: 'submit',
        });
    }

    /**
     * Баг Firefox:
     * Метод isDisplayedInViewport возвращает true для прозрачных элементов (opacity: 0)
     * Поведение не соотвествует документации - https://webdriver.io/docs/api/element/isDisplayedInViewport/
     *
     * Тикет на починку в очереди инфры - https://st.yandex-team.ru/INFRADUTY-19492
     * После починки, можно будет убрать переопределение стандартного метода.
     */
    async isDisplayedInViewport(): Promise<boolean> {
        if ((await this.browser.getBrowserName()) === 'firefox') {
            const opacity = await this.getCssProperty('opacity');

            if (opacity?.value === 0) {
                return false;
            }
        }

        return await super.isDisplayedInViewport();
    }
}
