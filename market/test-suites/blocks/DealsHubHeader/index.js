import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок DealsHubHeader
 * @param {PageObject.DealsHubHeader} dealsHubHeader
 */
export default makeSuite('Заголовок промохаба.', {
    story: {
        'по умолчанию': {
            'отображает текст для всех категорий': makeCase({
                issue: 'MARKETVERSTKA-34074',
                id: 'marketfront-3406',
                async test() {
                    const text = await this.dealsHubHeader.getHeaderText();

                    return this.expect(text).to.be.equal(
                        'Выгодно прямо сейчас',
                        'Текст заголовка корректный'
                    );
                },
            }),
        },
    },
});
