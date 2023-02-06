import {makeSuite, makeCase} from 'ginny';

const PICKER_INDEX = 2;
const TIME_TO_WAIT = 30000;

/**
 * @param {PageObject.ColorFilter} colorFilter
 * @param {PageObject.ProductSummary} productSummary
 * @param {PageObject.ProductSticker} productSticker
 * @param {PageObject.Preloadable} preloadable
 */
export default makeSuite('Пикер цвета. Изменения офферов.', {
    feature: 'Цвет на КМ',
    story: {
        'Меняется ТОП6 при смене цвета': makeCase({
            id: 'marketfront-2056',
            async test() {
                if (this.params.skuState) {
                    await this.browser.setState('report', this.params.skuState);
                }

                return this.colorFilter.getColorByIndex(PICKER_INDEX)
                    .then(currentColor => this.colorFilter.selectColor(PICKER_INDEX)
                        .then(() => this.preloadable.waitForLoaded())
                        .then(() => this.topOffer.mouseOver())
                        .then(() => this.browser.waitUntil(
                            () => this.topOffer.getItemSpecValue(),
                            TIME_TO_WAIT
                        ))
                        .then(topOffersColors => this.expect(topOffersColors)
                            .to
                            .deep
                            .equal(
                                Array.isArray(topOffersColors)
                                    ? Array(topOffersColors.length).fill(currentColor)
                                    : currentColor,
                                'цвета в пикере и ТОП6 совпадают'
                            )
                        )
                    );
            },
        }),
    },
});
