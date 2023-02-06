import {makeCase, mergeSuites, makeSuite} from 'ginny';

const ITEM_PROP_VALUE = 'itemListElement';
const ITEM_TYPE_VALUE = 'https://schema.org/ListItem';

/**
 * Тест на блок n-breadcrumbs.
 * @param {PageObject.Breadcrumbs} breadcrumbs
 */

export default mergeSuites(
    makeSuite('Хлебные крошки.', {
        environment: 'testing',
        feature: 'SEO',
        params: {
            urlMatcher: 'Регулярное выражение для проверки структуры ссылки из schema.org разметки',
        },
        defaultParams: {
            urlMatcher: '(catalog)|(product--.*)/\\d+(\\/list)?(\\?.*)?',
        },
        story: mergeSuites(
            {
                'По умолчанию': {
                    'должны содержать разметку schema.org.': makeCase({
                        id: 'marketfront-2559',
                        issue: 'MARKETVERSTKA-27115',
                        test() {
                            return this.breadcrumbs.itemLinks
                                .then(({value}) => value)
                                .then(links => links.reduce((chainPromise, link, index) => {
                                    const itemOrderIndex = index + 1;

                                    return chainPromise
                                        .then(() => this.breadcrumbs.getItemTextByIndex(itemOrderIndex))
                                        .then(text => Promise.all([
                                            text,
                                            this.breadcrumbs.getItemSeoDataByIndex(itemOrderIndex),
                                        ]))
                                        // INFO: проверяем поля отдельно для вывода нормального сообщения при ошибке
                                        .then(([itemText, seoData]) => this.expect(seoData)
                                            .to.have.own.property(
                                                'metaName',
                                                itemText,
                                                `Мета-имя крошки совпадает с названием "${itemText}".`
                                            )
                                            .then(() => this.expect(seoData)
                                                .to.have.own.property(
                                                    'itemprop',
                                                    ITEM_PROP_VALUE,
                                                    `Значение тега itemprop совпадает с "${ITEM_PROP_VALUE}".`
                                                )
                                            )
                                            .then(() => this.expect(seoData)
                                                .to.have.own.property(
                                                    'itemscope',
                                                    'true',
                                                    'Тег itemscope присутствует в разметке.'
                                                )
                                            )
                                            .then(() => this.expect(seoData)
                                                .to.have.own.property(
                                                    'itemtype',
                                                    ITEM_TYPE_VALUE,
                                                    `Значение тега itemtype совпадает с "${ITEM_TYPE_VALUE}".`
                                                )
                                            )
                                            .then(() => this.expect(seoData)
                                                .to.have.own.property(
                                                    'position',
                                                    String(itemOrderIndex),
                                                    `Значение тега position совпадает с "${itemOrderIndex}".`
                                                )
                                            )
                                            .then(() => this.expect(seoData)
                                                .to.have.own.property('metaUrl')
                                                .that.is.link(
                                                    {pathname: this.params.urlMatcher},
                                                    {
                                                        mode: 'match',
                                                        skipProtocol: true,
                                                        skipHostname: true,
                                                    }
                                                )
                                            ));
                                }, Promise.resolve()));
                        },
                    }),
                },
            }
        ),
    })
);
