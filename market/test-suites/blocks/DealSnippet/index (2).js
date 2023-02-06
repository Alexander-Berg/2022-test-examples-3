import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок DealSnippet
 * @property {PageObject.DealSnippet} dealSnippet
 */
export default makeSuite('Сниппет списка акций.', {
    params: {
        'fesh': 'Идентификатор магазина',
        'filter-by-promo-id': 'Идентификатор акции',
    },
    story: {
        'По умолчанию': {
            'является ссылкой на страницу отдельной акции': makeCase({
                issue: 'MOBMARKET-13217',
                id: 'm-touch-3006',
                async test() {
                    const href = await this.dealSnippet.getHref();

                    await this.expect(href).to.be.link({
                        pathname: '/search',
                        query: {
                            'fesh': this.params.fesh,
                            'filter-by-promo-id': this.params.promoId,
                        },
                    }, {
                        mode: 'match',
                        skipProtocol: true,
                        skipHostname: true,
                    });
                },
            }),
        },
    },
});
