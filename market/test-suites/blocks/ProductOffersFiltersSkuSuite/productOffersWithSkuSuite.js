import {makeSuite, makeCase} from 'ginny';

import {isEqual} from 'ambar';
import SnippetOffer from '@self/platform/components/ProductOffers/Snippet/Offer/__pageObject';
import {getLastReportRequestParams} from '@self/platform/spec/hermione/helpers/getBackendRequestParams';
import Paginator from '@self/platform/components/Paginator/__pageObject';
import Title from '@self/platform/components/PageCardTitle/Title/__pageObject';

import {clickOnFilterAndWaitListUpdate} from './helpers';


export default makeSuite('Страница цен без параметра sku', {
    story: {
        'По клику на фильтр со связанным sku': {
            'Обновляется список предложений,': makeCase({
                id: 'marketfront-5141',
                issue: 'MARKETFRONT-58266',
                async test() {
                    const initialCardsLength = await this.snippetList.getSnippetCardsLength(SnippetOffer);

                    await clickOnFilterAndWaitListUpdate({
                        filterColors: this.filterColors,
                        browser: this.browser,
                        snippetList: this.snippetList,
                        skuState: this.params.skuState,
                    });
                    const updatedCardsLength = await this.snippetList.getSnippetCardsLength(SnippetOffer);

                    return this.expect(isEqual(initialCardsLength, updatedCardsLength)).to.be.equal(false, 'Предложения обновились');
                },

            }),
            'Обновляется ДО в шапке,': makeCase({
                id: 'marketfront-5142',
                issue: 'MARKETFRONT-58266',
                async test() {
                    const initialDefaultOfferPrice = await this.price.getPriceText();

                    await clickOnFilterAndWaitListUpdate({
                        filterColors: this.filterColors,
                        browser: this.browser,
                        snippetList: this.snippetList,
                        skuState: this.params.skuState,
                    });

                    const updatedDefaultOfferPrice = await this.price.getPriceText();

                    return this.expect(isEqual(initialDefaultOfferPrice, updatedDefaultOfferPrice)).to.be.equal(false, 'Предложения обновились');
                },

            }),
            'Тайтл модели обновляется на тайтл SKU': makeCase({
                id: 'marketfront-5143',
                issue: 'MARKETFRONT-58266',
                async test() {
                    await clickOnFilterAndWaitListUpdate({
                        filterColors: this.filterColors,
                        browser: this.browser,
                        snippetList: this.snippetList,
                        skuState: this.params.skuState,
                    });

                    const title = await this.browser.getText(Title.root);

                    return this.expect(isEqual(title, this.params.skuTitle)).to.be.equal(true, 'Тайтл обновился');
                },

            }),
            'Пробрасываются параметры sku и glfilter в урл страницы': makeCase({
                id: 'marketfront-5144',
                issue: 'MARKETFRONT-58266',
                async test() {
                    await clickOnFilterAndWaitListUpdate({
                        filterColors: this.filterColors,
                        browser: this.browser,
                        snippetList: this.snippetList,
                        skuState: this.params.skuState,
                    });

                    const {query} = await this.browser.yaParseUrl();

                    return this.expect(Boolean(query.sku && query.glfilter))
                        .to.be.equal(true, 'параметры находятся в url');
                },
            }),
            'В репорт уходит запрос с правильными параметрами': makeCase({
                id: 'marketfront-5145',
                issue: 'MARKETFRONT-58266',
                async test() {
                    await clickOnFilterAndWaitListUpdate({
                        filterColors: this.filterColors,
                        browser: this.browser,
                        snippetList: this.snippetList,
                        skuState: this.params.skuState,
                    });

                    const {'market-sku': skuId} = await getLastReportRequestParams(this, 'productoffers');

                    return this.expect(Number(skuId)).to.not.be.NaN;
                },

            }),
            'Пробрасывается sku в пагинатор страница': makeCase({
                id: 'marketfront-5146',
                issue: 'MARKETFRONT-58266',
                async test() {
                    await clickOnFilterAndWaitListUpdate({
                        filterColors: this.filterColors,
                        browser: this.browser,
                        snippetList: this.snippetList,
                        skuState: this.params.skuState,
                    });

                    const href = await this.browser.getAttribute(`${Paginator.buttonNumber}:first-child`, 'href');

                    return this.expect(href.includes(`sku=${this.params.skuYellowId}`))
                        .to.be.equal(true, 'sku параметр находится в url');
                },
            }),
            'Пробрасывается sku в пагинатор далее': makeCase({
                id: 'marketfront-5147',
                issue: 'MARKETFRONT-58266',
                async test() {
                    await clickOnFilterAndWaitListUpdate({
                        filterColors: this.filterColors,
                        browser: this.browser,
                        snippetList: this.snippetList,
                        skuState: this.params.skuState,
                    });

                    const href = await this.browser.getAttribute(Paginator.buttonNext, 'href');

                    return this.expect(href.includes(`sku=${this.params.skuYellowId}`))
                        .to.be.equal(true, 'sku параметр находится в url');
                },
            }),
            'Пробрасывается sku в url в ссылке на "Ещё N вариантов», при кол предложений больше 48': makeCase({
                id: 'marketfront-5148',
                issue: 'MARKETFRONT-58266',
                async test() {
                    await clickOnFilterAndWaitListUpdate({
                        filterColors: this.filterColors,
                        browser: this.browser,
                        snippetList: this.snippetList,
                        skuState: this.params.skuState,
                    });

                    const href = await this.moreOffersLink.getHref();

                    return this.expect(href[0].includes(`sku=${this.params.skuYellowId}`)).to.be.equal(true, 'sku параметр находится в url');
                },
            }),

            'Выставляются все связанные фильтры': makeCase({
                id: 'marketfront-5149',
                issue: 'MARKETFRONT-58266',
                async test() {
                    await clickOnFilterAndWaitListUpdate({
                        filterColors: this.filterColors,
                        browser: this.browser,
                        snippetList: this.snippetList,
                        skuState: this.params.skuState,
                    });

                    const isSelected = await this.filterRadio.isFilterSelected();

                    return this.expect(isSelected).to.be.equal(true, 'связанные фильтры выставились');
                },
            }),
        },
    },
});
