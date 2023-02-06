import {Moment} from 'moment';

import parseDatesPeriod from 'helpers/utilities/date/parseDatesPeriod';
import dateFormats from 'helpers/utilities/date/formats';

import {Component} from 'components/Component';
import {TestHotelsCrossSaleMap} from 'components/TestHotelsCrossSaleMap';
import {TestLink} from 'components/TestLink';

export class TestHotelsCrossSaleMapCard extends Component {
    readonly crossSaleMap: TestHotelsCrossSaleMap;
    readonly link: TestLink;
    readonly hotelsCount: Component;
    readonly city: Component;
    readonly additionalTitle: Component;
    readonly selectHotelLink: TestLink;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.link = new TestLink(this.browser, {
            parent: this.qa,
            current: 'link',
        });

        this.hotelsCount = new Component(this.browser, {
            parent: this.qa,
            current: 'hotelsCount',
        });

        this.city = new Component(this.browser, {
            parent: this.qa,
            current: 'city',
        });

        this.additionalTitle = new Component(this.browser, {
            parent: this.qa,
            current: 'additionalTitle',
        });

        this.selectHotelLink = new TestLink(this.browser, {
            parent: this.qa,
            current: 'selectHotelLink',
        });

        this.crossSaleMap = new TestHotelsCrossSaleMap(this.browser, {
            parent: this.qa,
            current: 'crossSaleMap',
        });
    }

    async getDates(): Promise<{startDate: Moment; endDate: Moment}> {
        const additionalTitle = await this.additionalTitle.getText();

        if (this.isDesktop) {
            return parseDatesPeriod(additionalTitle, {
                monthFormat: dateFormats.MONTH,
                separator: 'по',
            });
        }

        const [, dates] = additionalTitle.split('₽').map(s => s.trim());

        return parseDatesPeriod(dates, {
            monthFormat: dateFormats.MONTH,
            separator: 'по',
        });
    }
}
