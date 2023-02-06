import {makeSuite, makeCase} from 'ginny';
import url from 'url';


/**
 * Тест на взаимодействие фильтров с деревом категорий.
 * @property {PageObject} searchIntent
 */
export default makeSuite('Ссылки в дереве категорий.', {
    environment: 'testing',
    params: {
        queryParamName: 'Имя параметра, которое будем проверять',
        queryParamValue: 'Значение параметра, которое будем проверять',
    },
    story: {
        'Ссылки содержат фильтр кластера как в урле': makeCase({
            id: 'marketfront-3346',
            issue: 'MARKETVERSTKA-33654',
            test() {
                const {queryParamName, queryParamValue} = this.params;

                return this.browser.yaWaitForPageReady()
                    .then(() => this.searchIntent.getSearchIntentLinkHref())
                    .then(linkHref => {
                        const parsedLink = url.parse(linkHref, true);
                        const {query: {[queryParamName]: paramFromLink}} = parsedLink;

                        return this.expect(paramFromLink)
                            .to.be.equal(queryParamValue, `Параметр ${queryParamName} пробрасывается в ссылку`);
                    });
            },
        }),
    },
});
