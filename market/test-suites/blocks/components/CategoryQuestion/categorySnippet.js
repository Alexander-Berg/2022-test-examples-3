import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на сниппет товара
 * @property {PageObject.CategorySnippet} this.categorySnippet
 */
export default makeSuite('Сниппет категории.', {
    params: {
        expectedUrl: 'ожидаемый адрес',
    },
    story: {
        'По умолчанию': {
            'содержит корректную ссылку': makeCase({
                feature: 'Структура страницы',
                async test() {
                    const link = await this.categorySnippet.getLink();

                    return this.expect(link, 'Сниппет категории содержит корректную ссылку')
                        .to.be.link(this.params.expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
