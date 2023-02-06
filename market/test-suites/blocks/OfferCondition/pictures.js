import {makeSuite, makeCase, mergeSuites} from 'ginny';

/**
 * @param {this.params.expectedPicturesCount}
 * @property {PageObject.OfferCondition} this.offerCondition
 */
export default makeSuite('Информация об изображениях уценённого товара', {
    params: {
        expectedPicturesCount: 'Ожидаемое количество фотографий уценённого товара',
    },
    feature: 'б/у товары',
    story: mergeSuites(
        {
            'Фотографии': {
                'по умолчанию': {
                    'присутствует в блоке': makeCase({
                        async test() {
                            if (!this.params.expectedPicturesCount) {
                                return;
                            }

                            const picturesCount = await this.offerCondition.getImagesCount();
                            await this.expect(picturesCount).to.be.equal(
                                this.params.expectedPicturesCount,
                                `Количество фотографий товара должно быть равно ${this.params.expectedPicturesCount}`
                            );
                        },
                    }),
                },
            },
        }
    ),
});
