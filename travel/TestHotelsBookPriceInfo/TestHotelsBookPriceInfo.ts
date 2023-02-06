import {Button, ComponentArray} from 'helpers/project/common/components';
import {TestHotelsCancellationInfo} from 'helpers/project/hotels/components/TestHotelsCancellationInfo/TestHotelsCancellationInfo';
import TestPriceGroupItem from 'helpers/project/hotels/pages/TestHotelsBookPage/components/TestHotelsBookPriceInfo/components/TestPriceGroupItem/TestPriceGroupItem';
import TestPlusInfo from 'helpers/project/hotels/pages/TestHotelsBookPage/components/TestHotelsBookPriceInfo/components/TestPlusInfo/TestPlusInfo';

import {Component} from 'components/Component';
import {TestPromoCodes} from './components/TestPromoCodes/TestPromoCodes';
import {TestDeferredPayment} from './components/TestDeferredPayment/TestDeferredPayment';
import {TestPrice} from 'components/TestPrice';

export class TestHotelsBookPriceInfo extends Component {
    readonly title: Component;
    readonly promoCodes: TestPromoCodes;
    readonly deferredPayment: TestDeferredPayment;
    readonly totalPrice: TestPrice;
    readonly totalPriceLabel: Component;
    readonly submitButton: Button;
    readonly taxiBadge: Component;
    readonly taxiNotAvailableText: Component;
    readonly cancellationInfo: TestHotelsCancellationInfo;
    readonly nightCountButton: Button;
    readonly priceGroupsItems: ComponentArray<TestPriceGroupItem>;
    readonly extraTaxes: ComponentArray<TestPriceGroupItem>;
    readonly includedTax: TestPriceGroupItem;
    readonly nightPrices: ComponentArray<TestPriceGroupItem>;
    readonly nightsTotal: TestPriceGroupItem;
    readonly discount: TestPriceGroupItem;
    readonly plusInfo: TestPlusInfo;

    constructor(browser: WebdriverIO.Browser, qa?: QA) {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });
        this.promoCodes = new TestPromoCodes(browser, {
            parent: this.qa,
            current: 'promoCodes',
        });
        this.submitButton = new Button(browser, {
            parent: this.qa,
            current: 'submit',
        });
        this.totalPriceLabel = new Component(browser, {
            parent: this.qa,
            current: 'totalPriceLabel',
        });
        this.totalPrice = new TestPrice(browser, {
            parent: this.qa,
            current: 'totalPrice',
        });
        this.taxiBadge = new Component(browser, {
            parent: this.qa,
            current: 'taxiBadge',
        });
        this.taxiNotAvailableText = new Component(browser, {
            parent: this.qa,
            current: 'taxiNotAvailableText',
        });
        this.deferredPayment = new TestDeferredPayment(browser, {
            parent: this.qa,
            current: 'deferredPayment',
        });
        this.cancellationInfo = new TestHotelsCancellationInfo(browser, {
            parent: this.qa,
            current: 'cancellationInfo',
        });
        this.nightCountButton = new Button(browser, {
            parent: this.qa,
            current: 'nightCountButton',
        });
        this.priceGroupsItems = new ComponentArray(
            browser,
            {parent: this.qa, current: ''},
            TestPriceGroupItem,
        );
        this.extraTaxes = new ComponentArray(
            browser,
            {parent: this.qa, current: 'extraTax'},
            TestPriceGroupItem,
        );
        this.includedTax = new TestPriceGroupItem(browser, {
            parent: this.qa,
            current: 'includedTax',
        });
        this.nightPrices = new ComponentArray(
            browser,
            {parent: this.qa, current: 'nightPrice'},
            TestPriceGroupItem,
        );
        this.nightsTotal = new TestPriceGroupItem(browser, {
            parent: this.qa,
            current: 'nightsTotal',
        });
        this.discount = new TestPriceGroupItem(browser, {
            parent: this.qa,
            current: 'discount',
        });
        this.plusInfo = new TestPlusInfo(browser, {
            parent: this.qa,
            current: 'plusInfo',
        });
    }
}
