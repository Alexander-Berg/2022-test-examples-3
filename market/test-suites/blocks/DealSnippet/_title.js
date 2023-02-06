import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на тексты компонента DealSnippet
 * @param {PageObject.DealSnippet} dealSnippet
 */
export default makeSuite('Текст сниппета списка акций.', {
    params: {
        expectedTitle: 'Ожидаемый текст сниппета',
    },
    story: {
        'в зависимости от типа акции': {
            'корректно отображается': makeCase({
                async test() {
                    const title = await this.dealSnippet.getTitle();

                    await this.expect(title).to.equal(this.params.expectedTitle);
                },
            }),
        },
    },
});
