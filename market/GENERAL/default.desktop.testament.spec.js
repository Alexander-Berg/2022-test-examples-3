// @flow

import {makeMirror} from '@self/platform/helpers/testament';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';

// flowlint-next-line untyped-import: off
import {BASE_URL, checkMobileLinkInFooter} from './testCases';

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
    describe('По умолчанию', () => {
        it('должен содержать содержать ссылку на мобильную версию, которая совпадает с переданной', async () => {
            await checkMobileLinkInFooter(
                {},
                apiaryLayer,
                kadavrLayer,
                mandrelLayer,
                BASE_URL
            );
        });
    });
});
