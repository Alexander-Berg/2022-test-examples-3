import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на уценку в визитке КМ
 * @property {PageObject.DiscountFilter} discountFilter - Фильтр уценки в визитке
 */
export default makeSuite('Блок визитки', {
    story: {
        'Описание': {
            'При выбранном фильтре "Уцененные"': {
                'содержит лэйбл "Уценённый товар:"': makeCase({
                    id: 'marketfront-3109',
                    issue: 'MARKETVERSTKA-32611',
                    async test() {
                        const titleText = await this.discountFilter.getTitleText();

                        await this.expect(titleText.toUpperCase()).to.be.equal(
                            'СОСТОЯНИЕ:\nУЦЕНЁННЫЙ',
                            'Правильный заголовок фильтра уценки'
                        );

                        const checkedRadioText = await this.discountFilter.getCheckedFilterText();

                        return this.expect(checkedRadioText.toUpperCase()).to.include(
                            'УЦЕНЁННЫЙ',
                            'Правильный заголовок фильтра уценки'
                        );
                    },
                }),
            },
        },
    },
});
