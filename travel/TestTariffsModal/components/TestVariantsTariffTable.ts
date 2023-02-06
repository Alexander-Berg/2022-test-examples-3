import TestTariffItem from 'helpers/project/avia/components/TestTariffsModal/components/TestTariffItem';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export default class TestVariantsTariffTable extends Component {
    title: Component;
    tariffs: ComponentArray<TestTariffItem>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });

        this.tariffs = new ComponentArray(
            browser,
            {parent: this.qa, current: 'tariff'},
            TestTariffItem,
        );
    }
}
