import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на description html-страницы.
 * @property {PageObject.PageMeta} pageMeta
 */
export default makeSuite('Description страницы.', {
    feature: 'SEO',
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'должен быть равен указанной строке.': makeCase({
                issue: 'MARKETVERSTKA-27672',
                id: 'marketfront-2316',
                params: {
                    expectedDescription: 'Ожидаемое значение для description страницы.',
                    matchMode: 'Флаг для переключения режима сравнения строк.',
                },
                defaultParams: {
                    matchMode: false,
                },
                async test() {
                    const {expectedDescription, matchMode} = this.params;
                    const result = await this.pageMeta.getPageDescription();

                    // INFO: если флаг взведен, то проверяем полученный description переданным регулярным выражением
                    if (matchMode) {
                        return this.expect(result).to.match(expectedDescription, `Description открытой страницы должен соответствовать выражению "${expectedDescription}"`);
                    }
                    // INFO: в противном случае (по умолчанию) просто сравниваем строки
                    return this.expect(result).to.be.equal(expectedDescription, `Description открытой страницы должен быть равен "${expectedDescription}"`);
                },
            }),
        },
    },
});
