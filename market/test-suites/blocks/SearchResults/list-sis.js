import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.SearchSnippetDelivery} snippetDelivery – Информация о доставке в сниппете
 */
export default makeSuite('Сниппет доставки.', {
    params: {
        deliveryText: 'Текст доставки',
    },
    story: {
        'По умолчанию': {
            ' информация о доставке представлена в соответствующем формату виде.': makeCase({
                async test() {
                    const {deliveryText} = this.params;

                    const text = await this.snippetDelivery.getText();
                    return text.should.be.equal(deliveryText, 'Текст должен соответствовать ожидаемому');
                },
            }),
        },
    },
});
