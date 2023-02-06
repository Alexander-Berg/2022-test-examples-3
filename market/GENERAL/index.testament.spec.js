import {getByTestId, getAllByTestId} from '@testing-library/dom';

import {makeMirror} from '@self/platform/helpers/testament';

import {
    HID,
    mockBreadcrums,
    mockFilters,
    mockFiltersDescription,
} from './mock';
import {
    GLOSSARY_LIST_TEST_ID,
    GLOSSARY_ITEM_TEST_ID,
    GLOSSARY_TERM_TEST_ID,
    GLOSSARY_DESCRIPTION_TEST_ID,
} from './constants';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;

async function makeContext({exps = {}, user = {}, params = {}} = {}) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user: {
            ...user,
        },
        request: {
            cookie,
            params,
            abt: {
                expFlags: exps || {},
            },
        },
    });
}

describe('Widget: FaqPage', () => {
    beforeAll(async () => {
        mirror = await makeMirror({
            jest: {
                testFilename: __filename,
                jestObject: jest,
            },
        });
        mandrelLayer = mirror.getLayer('mandrel');
        kadavrLayer = mirror.getLayer('kadavr');
        apiaryLayer = mirror.getLayer('apiary');
        jestLayer = mirror.getLayer('jest');

        await jestLayer.backend.runCode((mockBreadcrums, mockFilters, mockFiltersDescription) => {
            require('@self/project/src/spec/unit/mocks/yandex-market/mandrel/resolver');
            const {unsafeResource} = require('@yandex-market/mandrel/resolver');
            const {createUnsafeResourceMockImplementation} = require('@self/root/src/spec/unit/mocks/resolvers/helpers');

            unsafeResource.mockImplementation(
                createUnsafeResourceMockImplementation({
                    'cataloger.getNavigationPath': () => Promise.resolve(mockBreadcrums),
                    'buker.getFiltersDescription': () => Promise.resolve(mockFiltersDescription),
                    'report.search': () => Promise.resolve(mockFilters),
                })
            );
        }, [mockBreadcrums, mockFilters, mockFiltersDescription,
        ]);
    });

    beforeEach(async () => {
        await makeContext({params: {
            hid: HID,
        }});
    });

    afterAll(() => {
        mirror.destroy();
    });

    describe('Термины.', () => {
        describe('Блок терминов.', () => {
            test('По умолчанию каждый термин содержит определение и описание', async () => {
                const {container} = await apiaryLayer.mountWidget('..');

                const glossaryList = getByTestId(container, GLOSSARY_LIST_TEST_ID);
                const glossaryItems = getAllByTestId(glossaryList, GLOSSARY_ITEM_TEST_ID);

                glossaryItems.forEach(item => {
                    expect(getByTestId(item, GLOSSARY_TERM_TEST_ID).textContent).toBeTruthy();
                    expect(getByTestId(item, GLOSSARY_DESCRIPTION_TEST_ID).textContent).toBeTruthy();
                });
            });
        });
    });
});
