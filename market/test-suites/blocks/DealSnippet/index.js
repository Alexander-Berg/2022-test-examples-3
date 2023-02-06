import url from 'url';
import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок DealSnippet
 * @param {PageObject.DealSnippet} dealSnippet
 */
export default makeSuite('Сниппет списка акций.', {
    story: {
        'По умолчанию': {
            'является ссылкой на страницу отдельной акции': makeCase({
                issue: 'MARKETVERSTKA-31586',
                id: 'marketfront-2936',
                async test() {
                    const href = await this.dealSnippet.getHref();
                    const {pathname, query} = url.parse(href, true);

                    await this.expect(pathname).to.equal('/search', 'Ссылка ведет на корректный роут выдачи');

                    await this.expect(query).to.eql({
                        'fesh': this.params.fesh,
                        'filter-by-promo-id': this.params.promoId,
                    }, 'Ссылка содержит корректные параметры');
                },
            }),
        },
    },
});
