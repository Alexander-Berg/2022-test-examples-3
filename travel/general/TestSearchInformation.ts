import {Component} from 'components/Component';

export class TestSearchInformation extends Component {
    readonly direction: Component;
    readonly when: Component;
    readonly passengersAndClass: Component;
    readonly lupa: Component;

    constructor(browser: WebdriverIO.Browser, qa = 'searchInformation') {
        super(browser, qa);

        this.direction = new Component(browser, {
            parent: this.qa,
            current: 'direction',
        });
        this.when = new Component(browser, {
            parent: this.qa,
            current: 'when',
        });
        this.passengersAndClass = new Component(browser, {
            parent: this.qa,
            current: 'passengersAndClass',
        });
        this.lupa = new Component(browser, {
            parent: this.qa,
            current: 'lupa',
        });
    }

    /**
     * Закрытие формы через скрол
     */
    async close(): Promise<void> {
        await this.browser.execute(() => {
            const scrollTop = document.documentElement.scrollTop;

            window.scrollTo({top: scrollTop + 500});
            window.scrollTo({top: scrollTop - 500});
        });
    }

    async getDirections(): Promise<string[]> {
        return (await this.direction.getText()).split(' — ');
    }
}
