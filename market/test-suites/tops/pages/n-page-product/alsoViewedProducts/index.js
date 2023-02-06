import {mergeSuites, makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок scrollBox.
 * @param {PageObject.ScrollBoxWidget} scrollBox
 */

export default makeSuite('Карусель.', {
    feature: 'Структура страницы',
    story: mergeSuites(
        {
            'Заголовок.': {
                'По-умолчанию': {
                    'содержит ожидаемый текст': makeCase({
                        id: 'marketfront-910',
                        params: {
                            title: 'Текст, ожидаемый в заголовке карусели',
                        },
                        test() {
                            return this.scrollBox.getTitleText()
                                .should.eventually.be.equal(this.params.title, 'Заголовок совпадает с ожидаемым');
                        },
                    }),
                },
            },
        }
    ),
});
