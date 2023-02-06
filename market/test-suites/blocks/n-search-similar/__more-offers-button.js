import {prepareSuite, makeSuite, makeCase, mergeSuites} from 'ginny';
import schema from 'js-schema';
import nodeConfig from '@self/platform/configs/current/node';

import MetricaClickSuite from '@self/platform/spec/hermione/test-suites/blocks/Metrica/click';
import SearchSimilar from '@self/platform/widgets/content/SearchSimilar/__pageObject';

/**
 * @param {PageObject.SearchSimilar} searchSimilar
 */
export default makeSuite('Кнопка перехода на выдачу', {
    params: {
        moreButtonText: 'Текст, ожидаемый в кнопке перехода на выдачу',
        moreButtonLinkPath: 'Path ссылки в кнопке перехода на выдачу',
        moreButtonLinkQuery: 'Параметры ссылки в в кнопке перехода на выдачу',
        zonePrefix: 'Префикс зоны Метрики, в которой находится текущий блок',
    },
    story: mergeSuites(
        {
            'Всегда': {
                'содержит ожидаемый текст': makeCase({
                    id: 'marketfront-3350',
                    issue: 'MARKETVERSTKA-33752',
                    async test() {
                        const titleText = await this.searchSimilar.getMoreButtonText();

                        await this.expect(titleText).to.be.equal(
                            this.params.moreButtonText,
                            'Текст кнопки перехода на выдачу совпадает с ожидаемым'
                        );
                    },
                }),

                'содержит ожидаемую ссылку': makeCase({
                    id: 'marketfront-3350',
                    issue: 'MARKETVERSTKA-33752',
                    async test() {
                        const moreButtonLink = await this.searchSimilar.getMoreButtonLink();

                        await this.expect(moreButtonLink).to.be.link({
                            pathname: this.params.moreButtonLinkPath,
                            query: this.params.moreButtonLinkQuery,
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
                issue: 'MARKETVERSTKA-33758',
                id: 'marketfront-3356',
            },
            hooks: {
                beforeEach() {
                    this.params = {
                        ...this.params,
                        counterId: nodeConfig.yaMetrika.market.id,
                        expectedGoalName: `${this.params.zonePrefix}_similar_more-offers-button_click`,
                        payloadSchema: schema({}),
                        selector: SearchSimilar.moreButton,
                        scrollOffset: [0, -50],
                    };
                },
            },
        })
    ),
});
