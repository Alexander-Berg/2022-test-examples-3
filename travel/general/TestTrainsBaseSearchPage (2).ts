import {retry} from 'helpers/project/common/retry';
import {TestTrainsSearchVariants} from 'helpers/project/trains/pages/TestTrainsGenericSearchPage/components/TestTrainsSearchVariants/TestTrainsSearchVariants';

import {Page} from 'components/Page';
import {Component} from 'components/Component';

export class TestTrainsBaseSearchPage extends Page {
    variants: TestTrainsSearchVariants;
    emptySerp: Component;

    constructor(browser: WebdriverIO.Browser, qa?: QA) {
        super(browser, qa);

        this.variants = new TestTrainsSearchVariants(browser);
        this.emptySerp = new Component(browser, 'no-trains');
    }

    async waitVariantsAndTariffsLoaded(): Promise<void> {
        try {
            await retry(
                async () => {
                    const variants = this.variants.variants;
                    const variantsCount = await variants.count();
                    const hasEmptySerp = await this.emptySerp.isDisplayed(0);

                    if (hasEmptySerp) {
                        return;
                    }

                    if (!variantsCount) {
                        throw new Error('Варианты еще не загрузились');
                    }

                    await variants.forEach(async variant => {
                        const hasPricesSpinner =
                            await variant.checkHasVariantPricesSpinner();

                        if (hasPricesSpinner) {
                            throw new Error(
                                'Тарифы не у всех вариантов загрузились',
                            );
                        }
                    });
                },
                {attempts: 50, delay: 2000},
            )();
        } catch (err) {
            throw new Error('Поллинг на поиске длится более 100 секунд');
        }
    }
}
