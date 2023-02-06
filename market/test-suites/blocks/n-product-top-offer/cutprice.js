/* eslint-disable no-unreachable */

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на уценку в оффере ТОП6 на КМ
 * @property {PageObject.ProductTopOffer} topOffer - Оффер листа ТОП 6
 * @property {PageObject.ClickoutButton} clickoutButton - Кнопка "В магазин" оффера листа ТОП 6
 */
export default makeSuite('Блок оффера ТОП 6', {
    story: {
        'Описание': {
            'При выбранном фильтре "Уцененные"': {
                'содержит лэйбл "Уценённый — подержанный."': makeCase({
                    id: 'marketfront-3540',
                    issue: 'MARKETVERSTKA-34746',
                    test() {
                        // eslint-disable-next-line market/ginny/no-skip
                        return this.skip('MARKETFRONT-40812 скипаем упавшие тесты ' +
                            'т к были оторваны табы КМ и потерялась точка входа');
                        return this.cutPriceDescription.getInfoText()
                            .should.eventually.to.include(
                                'Уценённый — подержанный.',
                                'Правильный текст в оффере ТОП 6'
                            );
                    },
                }),
            },
        },
    },
});
