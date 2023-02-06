import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на заголовок h1 на html-странице.
 * @property {PageObject.Headline} headline
 */

export default makeSuite('Заголовок h1 открытой страницы.', {
    environment: 'kadavr',
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'соответствует заданному значению.': makeCase({
                params: {
                    expectedHeaderText: 'Ожидаемое значение для h1-заголовка страницы.',
                },
                test() {
                    const {expectedHeaderText} = this.params;

                    return this.headline.getHeaderTitleText()
                        .should.eventually.be.equal(
                            expectedHeaderText,
                            `h1-заголовок страницы равен "${expectedHeaderText}"`
                        );
                },
            }),
        },
    },
});
