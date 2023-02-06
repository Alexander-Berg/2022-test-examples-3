import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-snippet-card2
 * @param {PageObject.SnippetCard2} snippetCard2
 */
export default makeSuite('Сниппет карточки.', {
    story: {
        'По умолчанию': {
            'Элемент выдачи должен иметь цену товара в указанном формате': makeCase({
                test() {
                    return this.snippetCard2
                        .getFullPrice()
                        .should.eventually.to.be.equal(
                            this.params.expectedPrice,
                            'Цена на элементе выдачи в формате "от <цена>"'
                        );
                },
            }),
        },
    },
});
