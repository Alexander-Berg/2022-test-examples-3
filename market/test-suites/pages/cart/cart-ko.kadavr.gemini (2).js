// helpers
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {hideHeadBanner} from '@self/platform/spec/gemini/helpers/hide';

// kadavr
import {createSession, setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {mergeState, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';
// mocks
import {
    shopInfoMock,
    cpaType1POfferMock,
    cpaType3POfferMock,
    cpaTypeDSBSOfferMock,
    catalogerMock,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/cpaOffer.mock';
import {generateCartSuites} from './cart.block';


function makeSuiteByType(offerMock, suiteName) {
    return {
        suiteName,
        url: `/offer/${offerMock.wareId}`,
        before(actions) {
            createSession.call(actions);
            setState.call(actions, 'Cataloger.tree', catalogerMock);
            setState.call(actions, 'Carter.items', []);
            setState.call(actions, 'ShopInfo.collections', shopInfoMock);
            setState.call(actions, 'report', mergeState([
                createOffer(offerMock, offerMock.wareId),
                {
                    data: {
                        search: {
                            total: 1,
                        },
                    },
                },
            ]));
            setDefaultGeminiCookies(actions);
            hideHeadBanner(actions);
            initLazyWidgets(actions);
        },
        after(actions) {
            deleteSession.call(actions);
        },
        childSuites: generateCartSuites('.main'),
    };
}

export default {
    suiteName: 'KO-Cart[KADAVR]',
    childSuites: [
        makeSuiteByType(cpaType1POfferMock, '1P'),
        makeSuiteByType(cpaType3POfferMock, '3P'),
        makeSuiteByType(cpaTypeDSBSOfferMock, 'DSBS'),
    ],
};
