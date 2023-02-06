import {makeSuite, mergeSuites, prepareSuite, makeCase} from 'ginny';
import schema from 'js-schema';

import nodeConfig from '@self/platform/configs/current/node';

import MetricaVisibleSuite from '@self/platform/spec/hermione/test-suites/blocks/Metrica/visible';

/**
 * Тесты на блок n-snippet-card с уценкой
 * @param {PageObject.SnippetCard} snippetCard
 * @param {PageObject.OfferInfo} offerInfo
 * @param {PageObject.TabsMenu} offerInfoTabsMenu
 */
export default makeSuite('Листовой сниппет с уценкой.', {
    story: mergeSuites(
        makeSuite('Метрика', {
            id: 'marketfront-3287',
            issue: 'MARKETVERSTKA-33045',

            story: prepareSuite(MetricaVisibleSuite, {
                hooks: {
                    async beforeEach() {
                        await this.snippetCard.waitForVisible();

                        this.params.selector = await this.snippetCard.getSelector();
                    },
                },
                params: {
                    expectedGoalName: 'product-offers-page_snippet-list-discounted_snippet-card_to-shop_visible',
                    counterId: nodeConfig.yaMetrika.market.id,
                    payloadSchema: schema({}),
                },
            }),
        }),
        {
            'Сниппет содержит лэйбл "Уценённый — подержанный."': makeCase({
                id: 'marketfront-3541',
                issue: 'MARKETVERSTKA-34747',
                async test() {
                    await this.snippetCard.waitForVisible();

                    const description = await this.snippetCard.getCutpriceDescription();

                    return this.expect(description).to.include(
                        'Уценённый — подержанный.',
                        'Оффер содержит лэйбл "Уценённый — подержанный."'
                    );
                },
            }),
            'При клике по "Читать далее"': {
                'Открывает попап на вкладке "Описание от продавца"': makeCase({
                    id: 'marketfront-3542',
                    issue: 'MARKETVERSTKA-34748',
                    async test() {
                        await this.snippetCard.waitForVisible();

                        await this.snippetCard.clickCutpriceDescriptionMore();
                        await this.offerInfoTabsMenu.waitForVisible();

                        this.offerInfoTabsMenu.getActiveTabText()
                            .should.eventually.be.equal(
                                'ОПИСАНИЕ ОТ ПРОДАВЦА',
                                'Должнен быть активным таб "Описание от продавца"'
                            );
                    },
                }),
            },
            'По умолчанию': {
                'Сниппет и описание в попапе содержат одинаковый лейбл "Уценённый — подержанный."': makeCase({
                    id: 'marketfront-3543',
                    issue: 'MARKETVERSTKA-34749',
                    async test() {
                        await this.snippetCard.waitForVisible();
                        await this.snippetCard.getCutpriceDescription()
                            .should.eventually.include(
                                'Уценённый — подержанный.',
                                'Оффер содержит лэйбл "Уценённый — подержанный."'
                            );

                        await this.snippetCard.clickCutpriceDescriptionMore();

                        await this.offerInfo.waitForVisible();
                        return this.offerInfo.getDescText()
                            .should.eventually.include(
                                'Уценённый — подержанный.',
                                'Описание оффера в попапе содержит лэйбл "Уценённый — подержанный."'
                            );
                    },
                }),
            },
        }
    ),
});
