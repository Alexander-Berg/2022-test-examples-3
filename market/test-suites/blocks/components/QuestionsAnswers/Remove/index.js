import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Remove} remove
 */
export default makeSuite('Кнопка «Удалить» по умолчанию', {
    feature: 'Структура страницы',
    story: {
        'Если элемент может быть удалён': {
            'кнопка доступна': makeCase({
                id: 'marketfront-2824',
                issue: 'MARKETVERSTKA-31044',
                async test() {
                    await this.remove
                        .isExisting()
                        .should.eventually.be.equal(true, 'Кнопка удаления присутствует');
                },
            }),
        },
    },
});
