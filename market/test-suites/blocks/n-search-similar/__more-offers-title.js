import {prepareSuite, makeSuite, makeCase, mergeSuites} from 'ginny';
import schema from 'js-schema';
import nodeConfig from '@self/platform/configs/current/node';

import MetricaClickSuite from '@self/platform/spec/hermione/test-suites/blocks/Metrica/click';
import SearchSimilar from '@self/platform/widgets/content/SearchSimilar/__pageObject';

/**
 * @param {PageObject.SearchSimilar} searchSimilar
 */
export default makeSuite('Заголовок', {
    params: {
        titleText: 'Текст, ожидаемый в заголовке блока',
        titleLinkPath: 'Path ссылки в заголовке блока',
        titleLinkQuery: 'Параметры ссылки в заголовке блока',
        zonePrefix: 'Префикс зоны Метрики, в которой находится текущий блок',
    },
    story: mergeSuites(
        {
            'Всегда': {
                'содержит ожидаемый текст': makeCase({
                    id: 'marketfront-3349',
                    issue: 'MARKETVERSTKA-33751',
                    async test() {
                        const titleText = await this.searchSimilar.getTitleText();

                        await this.expect(titleText).to.be.equal(
                            this.params.titleText,
                            'Заголовок совпадает с ожидаемым'
                        );
                    },
                }),

                'содержит ожидаемую ссылку': makeCase({
                    id: 'marketfront-3349',
                    issue: 'MARKETVERSTKA-33751',
                    async test() {
                        const titleLink = await this.searchSimilar.getTitleLink();

                        await this.expect(titleLink).to.be.link({
                            pathname: this.params.titleLinkPath,
                            query: this.params.titleLinkQuery,
                        }, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    },
                }),
            },
        },

        prepareSuite(MetricaClickSuite, {
            meta: {
                issue: 'MARKETVERSTKA-33757',
                id: 'marketfront-3355',
            },
            hooks: {
                beforeEach() {
                    this.params = {
                        ...this.params,
                        counterId: nodeConfig.yaMetrika.market.id,
                        expectedGoalName: `${this.params.zonePrefix}_similar_more-offers-title_click`,
                        payloadSchema: schema({}),
                        selector: SearchSimilar.titleLink,
                        scrollOffset: [0, -50],
                    };
                },
            },
        })
    ),
});
