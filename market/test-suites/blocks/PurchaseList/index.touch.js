import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок snippetPrice
 * @param {PageObject.SnippetPrice} snippetPrice
 */
export default makeSuite('Сниппет карточки.', {
    story: {
        'По умолчанию': {
            'Элемент выдачи должен иметь цену товара в указанном формате': makeCase({
                test() {
                    return this.snippetPrice
                        .getPrice()
                        .should.eventually.to.be.equal(
                            this.params.expectedPrice,
                            'Цена на элементе выдачи в формате "от <цена>"'
                        );
                },
            }),
        },
    },
});
