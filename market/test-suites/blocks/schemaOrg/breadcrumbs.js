import {makeSuite, makeCase} from 'ginny';

const BREADCRUMBS_LIST_ITEM_TYPE_VALUE = 'https://schema.org/BreadcrumbList';

/**
 * Тесты на разметку schema.org хлебных крошек.
 * @property {PageObject.SchemaOrgBreadcrumbsList} this.schemaOrgBreadcrumbsList
 */
export default makeSuite('Schema.org для хлебных крошек.', {
    environment: 'kadavr',
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'имеют все нужные атрибуты на основном элементе разметки.': makeCase({
                id: 'marketfront-2445',
                issue: 'MARKETVERSTKA-28895',
                async test() {
                    // INFO: выполняем последовательно для формирования читаемого allure-отчета
                    const schemaOrgAttrs = {
                        itemscope: await this.schemaOrgBreadcrumbsList.getItemScopeFromElem(),
                        itemtype: await this.schemaOrgBreadcrumbsList.getItemTypeFromElem(),
                    };

                    return this.expect(schemaOrgAttrs)
                        .to.have.own.property(
                            'itemscope',
                            'true',
                            'На главном элементе установлен атрибут "itemscope"'
                        )
                        .then(() => this.expect(schemaOrgAttrs)
                            .to.have.own.property(
                                'itemtype',
                                BREADCRUMBS_LIST_ITEM_TYPE_VALUE,
                                'На главном элементе установлен атрибут со значением ' +
                                `"${BREADCRUMBS_LIST_ITEM_TYPE_VALUE}".`
                            )
                        );
                },
            }),
        },
    },
});
