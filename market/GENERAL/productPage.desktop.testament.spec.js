// @flow

import {makeMirror} from '@self/platform/helpers/testament';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';

// flowlint-next-line untyped-import: off
import {checkMobileLinkInFooter, BASE_URL} from './testCases';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

let timing;

beforeAll(async () => {
    timing = window.performance.timing;
    window.performance.timing = {
        navigationStart: () => 0,
    };
    mockIntersectionObserver();
    mirror = await makeMirror({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            asLibrary: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');

    await jestLayer.runCode(() => {
        const {mockRouter} = require('@self/project/src/helpers/testament/mock');
        mockRouter();
    }, []);
});

afterAll(() => {
    mirror.destroy();
    window.performance.timing = timing;
});

describe('Виджет футера', () => {
    describe('На странице КМ', () => {
        beforeAll(async () => {
            await jestLayer.runCode(() => {
                jest.spyOn(require('@yandex-market/mandrel/resolvers/page'), 'resolvePageIdSync')
                    .mockReturnValue('market:product');
            }, []);
        });

        it('должен содержать содержать ссылку на мобильную версию, которая совпадает с переданной', async () => {
            const params = {
                slug: 'xiaomi-redmi-note-3-pro-32gb',
                productId: '13527763',
            };
            await checkMobileLinkInFooter(
                params,
                apiaryLayer,
                kadavrLayer,
                mandrelLayer,
                `${BASE_URL}/product--${params.slug}/${params.productId}`
            );
        });
    });
});
