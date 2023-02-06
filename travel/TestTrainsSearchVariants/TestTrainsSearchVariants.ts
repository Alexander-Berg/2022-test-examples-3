import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

import {TestTrainsSearchSegment} from '../TestTrainsSearchSegment/TestTrainsSearchSegment';
import {
    TestTrainsSearchVariant,
    TVariantAndSegmentOptions,
} from '../TestTrainsSearchVariant/TestTrainsSearchVariant';

export class TestTrainsSearchVariants extends Component {
    variants: ComponentArray<TestTrainsSearchVariant>;

    constructor(browser: WebdriverIO.Browser, qa?: QA) {
        super(browser, qa);

        this.variants = new ComponentArray(
            browser,
            'searchVariant',
            TestTrainsSearchVariant,
        );
    }

    async checkVariantsWithTransfer(): Promise<boolean> {
        return this.variants.every(variant => {
            return variant.checkIsVariantWithTransfer();
        });
    }

    async checkVariantsDirectionMainFields(): Promise<void> {
        await this.variants.forEach(async variant => {
            await variant.checkVariantDirectionMainFields();
        });
    }

    async checkVariantsSegments(): Promise<void> {
        const variantsCount = await this.variants.count();

        if (variantsCount < 2) {
            throw new Error(
                'Вариант с пересадкой должен содержать не менее 2 сегментов',
            );
        }

        for (const variant of await this.variants.items) {
            await variant.checkVariantSegments();
        }
    }

    async getVariantMinPrices(): Promise<number[]> {
        const variants = await this.variants.items;
        const variantPrices = [];

        for (const variant of variants) {
            const variantMinPrice = await variant.getVariantMinPrice();

            if (variantMinPrice) {
                variantPrices.push(variantMinPrice);
            }
        }

        return variantPrices;
    }

    async getVariantMinPrice(): Promise<number | null> {
        const minPrices = await this.getVariantMinPrices();

        if (!minPrices.length) {
            return null;
        }

        return Math.min(...minPrices);
    }

    async findVariantAndSegmentByOptions(
        variantAndSegmentOptions?: TVariantAndSegmentOptions,
    ): Promise<{
        variant: TestTrainsSearchVariant;
        segment: TestTrainsSearchSegment;
    } | null> {
        const variants = await this.variants.items;

        for (const variant of variants) {
            const isDisplayedVariant = await variant.isDisplayed(0);

            if (!isDisplayedVariant) {
                continue;
            }

            const segment = await variant.getVariantSegmentByOptions(
                variantAndSegmentOptions,
            );

            if (segment) {
                return {
                    variant,
                    segment,
                };
            }
        }

        return null;
    }
}
