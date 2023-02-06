import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

import {
    ETrainsSegmentTariffTitle,
    TestTrainsSearchSegmentTariff,
} from '../TestTrainsSearchSegmentTariff/TestTrainsSearchSegmentTariff';

export class TestTrainsSearchSegmentTariffs extends Component {
    tariffs: ComponentArray<TestTrainsSearchSegmentTariff>;

    constructor(browser: WebdriverIO.Browser, qa?: QA) {
        super(browser, qa);

        this.tariffs = new ComponentArray(
            browser,
            'tariff',
            TestTrainsSearchSegmentTariff,
        );
    }

    checkTariffsMainFields = async (): Promise<void> => {
        const hasTariffs = await this.isDisplayed(0);

        if (hasTariffs) {
            await this.tariffs.forEach(async tariff => {
                await tariff.checkTariffFields();
            });
        }
    };

    findTariffByTitle = async (
        tariffTitle: ETrainsSegmentTariffTitle,
    ): Promise<TestTrainsSearchSegmentTariff | null> => {
        const hasTariffs = await this.isDisplayed();

        if (!hasTariffs) {
            return null;
        }

        const tariffs = await this.tariffs.items;

        for (const tariff of tariffs) {
            const isTariffMatched = await tariff.isTariffMatchesToTitle(
                tariffTitle,
            );

            if (isTariffMatched) {
                return tariff;
            }
        }

        return null;
    };
}
