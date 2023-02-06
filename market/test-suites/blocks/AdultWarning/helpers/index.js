import _ from 'lodash';
import url from 'url';
import SnippetCard from '@self/platform/spec/page-objects/snippet-card';
import ProductTopOffer from '@self/platform/spec/page-objects/n-product-top-offer';
import SnippetCard2 from '@self/project/src/components/Search/Snippet/Card/__pageObject';
import SnippetOffer from '@self/platform/components/ProductOffers/Snippet/Offer/__pageObject';

const getSnippetsCount = async ctx => {
    const cardsCount = await ctx.snippetList.getSnippetCardsLength(SnippetCard);
    const updatedCardsCount = await ctx.snippetList.getSnippetCardsLength(SnippetCard2);
    const topOffersCount = await ctx.snippetList.getSnippetCardsLength(ProductTopOffer);
    const offersCount = await ctx.snippetList.getSnippetCardsLength(SnippetOffer);

    return cardsCount + updatedCardsCount + topOffersCount + offersCount;
};

const getLastReportRequestParams = async ctx => {
    const log = await ctx.browser.getLog();
    const {Report} = log;

    const lastReportRequest = _.findLast(Report, item => {
        const {request} = item;
        const {url: requestUrl} = request;

        return requestUrl.includes('place=prime') || requestUrl.includes('place=blender');
    });

    const lastReportRequestUrl = _.get(lastReportRequest, 'request.url');
    const {query} = url.parse(lastReportRequestUrl, true);

    return query;
};

export {
    getSnippetsCount,
    getLastReportRequestParams,
};
