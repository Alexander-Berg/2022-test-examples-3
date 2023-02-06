// @flow

import {makeMirror} from '@self/platform/helpers/testament';
import {createPurchasedGoodsFilterSuite} from '@self/project/src/widgets/content/PurchasedGoodsFilter/__spec__';

// page-objects
// flowlint-next-line untyped-import:off
import PurchasedGoodsFilter
    from '@self/project/src/widgets/content/PurchasedGoodsFilter/components/View/__pageObject/index.touch';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

const WIDGET_PATH = require.resolve('@self/project/src/widgets/content/PurchasedGoodsFilter');

async function makeContext() {
    const UID = '9876543210';
    const yandexuid = '1234567890';
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user: {
            UID,
            yandexuid,
            isAuth: true,
        },
        request: {
            cookie,
        },
    });
}

beforeAll(async () => {
    // $FlowFixMe<type of jest?>
    mirror = await makeMirror({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            asLibrary: true,
        },
    });
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');
});

afterAll(() => {
    mirror.destroy();
});

describe('Widget: PurchasedGoodsFilter', () => {
    createPurchasedGoodsFilterSuite(
        makeContext,
        WIDGET_PATH,
        PurchasedGoodsFilter.root,
        () => ({apiaryLayer, kadavrLayer})
    );
});
