/* eslint-disable global-require */

import {screen} from '@testing-library/dom';
import {makeMirrorDesktop as makeMirror} from '@self/root/src/helpers/testament/mirror';
import {mountSearchWidgets, SEARCH_ROOT_WIDGET_PATH} from '@self/root/src/widgets/content/search/__spec__/mocks/mountSearchWidgets';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {NON_BREAKING_SPACE_CHAR as NBSP} from '@self/root/src/constants/string';
import {
    mockBasicSearchFunctionality,
    mockNoopBackendFunctionality,
    mockSearchFunctionality,
    makeContext,
} from '../../__spec__/mocks';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

beforeAll(async () => {
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
    jestLayer = mirror.getLayer('jest');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');
});

describe('Search: Дисклеймер', () => {
    const pageId = PAGE_IDS_COMMON.SEARCH;
    const initialParams = {};

    beforeEach(async () => {
        await mockBasicSearchFunctionality({jestLayer}, pageId, initialParams);
        await mockNoopBackendFunctionality({jestLayer, kadavrLayer});
        await makeContext({kadavrLayer, mandrelLayer, jestLayer}, initialParams);

        await mockSearchFunctionality({kadavrLayer}, {commo: true});

        await apiaryLayer.mountWidget(
            SEARCH_ROOT_WIDGET_PATH,
            mountSearchWidgets([
                {widgetName: 'SearchLegalInfo', props: {}},
            ])
        );
    });

    describe('По умолчанию', () => {
        test('должны отображаться', () => {
            expect(screen.getByText(/Информацию об условиях отпуска/).textContent)
                .toEqual(`Информацию об${NBSP}условиях отпуска (реализации) уточняйте у${NBSP}продавца.`);
        });
    });
});
