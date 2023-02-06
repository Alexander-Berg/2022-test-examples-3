import {makeSuite, makeCase} from 'ginny';

const PICKER_INDEX = 2;

/**
 * @param {PageObject.ColorFilter} colorFilter
 * @param {PageObject.ProductSummary} productSummary
 * @param {PageObject.ProductSticker} productSticker
 * @param {PageObject.Preloadable} preloadable
 */
export default makeSuite('Пикер цвета. Изменения офферов.', {
    feature: 'Цвет на КМ',
    story: {
        'Меняется ДО при смене цвета': makeCase({
            id: 'marketfront-2055',
            async test() {
                if (this.params.skuState) {
                    await this.browser.setState('report', this.params.skuState);
                }

                await this.colorFilter.selectColor(PICKER_INDEX);

                await this.browser.waitUntil(
                    async () => {
                        const defaultOfferId = await this.defaultOffer.getOfferId();

                        return defaultOfferId === this.params.expectedOfferId;
                    },
                    5000,
                    'появился новый ДО'
                );
            },
        }),
    },
});
