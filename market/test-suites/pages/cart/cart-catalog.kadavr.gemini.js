// page-objects
import SearchResult from '@self/platform/spec/page-objects/SearchResult';
// helpers
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideHeadBanner} from '@self/platform/spec/gemini/helpers/hide';
import deleteCookie from '@yandex-market/gemini-extended-actions/actions/deleteCookie';
import {clone} from 'ambar';
// kadavr
import {createSession, setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {mergeState, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';
import {
    cpaType1POfferMock,
    cpaType3POfferMock,
    cpaTypeDSBSOfferMock,
    shopInfoMock,
    catalogerGridMock,
    catalogerListMock,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/cpaOffer.mock';
import {templatorTarantinoMock} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/snippetConfigurationCMS.mock';

// blocks
import {generateCartSuites} from './cart.block';

const gridSnippetConfiguration = clone(templatorTarantinoMock);
gridSnippetConfiguration.snippets[0].template.viewtype = 'grid';

const listSnippetConfiguration = clone(templatorTarantinoMock);
gridSnippetConfiguration.snippets[0].template.viewtype = 'list';

function makeSuiteByType(offerMock, suiteName) {
    return {
        suiteName,
        childSuites: [
            {
                suiteName: 'Grid',
                url: {
                    pathname: `/catalog--naushniki-i-bluetooth-garnitury/${offerMock.categories[0].nid}`,
                    query: {
                        hid: offerMock.categories[0].id,
                        onstock: 1,
                        'local-offers-first': 0,
                    },
                },
                before(actions) {
                    createSession.call(actions);
                    setState.call(actions, 'Cataloger.tree', catalogerGridMock);
                    setState.call(actions, 'Carter.items', []);
                    setState.call(actions, 'ShopInfo.collections', shopInfoMock);
                    setState.call(actions, 'Tarantino.data.result', [gridSnippetConfiguration]);
                    setState.call(actions, 'report', mergeState([
                        createOffer(offerMock, offerMock.wareId),
                        {
                            data: {
                                search: {
                                    total: 1,
                                    totalOffers: 1,
                                    view: 'grid',
                                },
                            },
                        },
                    ]));
                    setDefaultGeminiCookies(actions);
                    hideHeadBanner(actions);
                },
                after(actions) {
                    deleteSession.call(actions);
                    // Грязный хак - иначе редиректит в кривую категорию при попытке открыть урл в том же браузере
                    deleteCookie.call(actions, 'kadavr_session_id');
                    deleteCookie.call(actions, 'kadavr_host_port');
                },
                childSuites: generateCartSuites(SearchResult.root),
            },
            {
                suiteName: 'List',
                url: {
                    pathname: `/catalog--naushniki-i-bluetooth-garnitury/${offerMock.categories[0].nid}`,
                    query: {
                        hid: offerMock.categories[0].id,
                        onstock: 1,
                        'local-offers-first': 0,
                    },
                },
                before(actions) {
                    createSession.call(actions);
                    setState.call(actions, 'Cataloger.tree', catalogerListMock);
                    setState.call(actions, 'Carter.items', []);
                    setState.call(actions, 'ShopInfo.collections', shopInfoMock);
                    setState.call(actions, 'Tarantino.data.result', [listSnippetConfiguration]);
                    setState.call(actions, 'report', mergeState([
                        createOffer(offerMock, offerMock.wareId),
                        {
                            data: {
                                search: {
                                    total: 1,
                                    totalOffers: 1,
                                    view: 'list',
                                },
                            },
                        },
                    ]));
                    setDefaultGeminiCookies(actions);
                    hideHeadBanner(actions);
                },
                after(actions) {
                    deleteSession.call(actions);
                    // Грязный хак - иначе редиректит в кривую категорию при попытке открыть урл в том же браузере
                    deleteCookie.call(actions, 'kadavr_session_id');
                    deleteCookie.call(actions, 'kadavr_host_port');
                },
                childSuites: generateCartSuites(SearchResult.root),
            },
        ],
    };
}

export default {
    suiteName: 'KO-Catalog[KADAVR]',
    childSuites: [
        makeSuiteByType(cpaType1POfferMock, '1P'),
        makeSuiteByType(cpaType3POfferMock, '3P'),
        makeSuiteByType(cpaTypeDSBSOfferMock, 'DSBS'),
    ],
};
