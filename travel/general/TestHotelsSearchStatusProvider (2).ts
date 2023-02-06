import {Component} from 'components/Component';

const PAGE_LOAD_DEFAULT_TIMEOUT = 5000;
const SEARCH_HOTELS_DEFAULT_TIMEOUT = 30000;

export class TestHotelsSearchStatusProvider extends Component {
    private searchIsPending: Component;
    private searchIsFinished: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.searchIsPending = new Component(browser, {
            parent: this.qa,
            current: 'searchIsPending',
        });
        this.searchIsFinished = new Component(browser, {
            parent: this.qa,
            current: 'searchIsFinished',
        });
    }

    async waitLoading(): Promise<void> {
        try {
            /**
             * Вместо результатов выдачи, карты, области фильтров поначалу отображаются скелетоны
             */
            await this.isPending();
        } catch (e) {
            /**
             * скелетоны не успели появится, работаем дальше
             */
        }

        /**
         * Через какое то время (до 10 секунд как правило) скелетоны пропадают
         */
        await this.isFinished();
    }

    private isPending(timeout = PAGE_LOAD_DEFAULT_TIMEOUT): Promise<void> {
        return this.searchIsPending.waitForVisible(timeout);
    }

    private isFinished(timeout = SEARCH_HOTELS_DEFAULT_TIMEOUT): Promise<void> {
        return this.searchIsFinished.waitForVisible(timeout);
    }
}
