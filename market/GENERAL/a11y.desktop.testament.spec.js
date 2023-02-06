// @flow
// flowlint untyped-import:off

import {packFunction} from '@yandex-market/testament/mirror';
import {getByText} from '@testing-library/dom';

import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver, mockLocation} from '@self/root/src/helpers/testament/mock';

import HeaderTabs from '@self/platform/widgets/content/HeaderTabs/__pageObject';

import {nodes} from './__mock__/nodes';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

const widgetPath = require.resolve('..');

let timing;

beforeAll(async () => {
    timing = window.performance.timing;
    window.performance.timing = {
        navigationStart: () => 0,
    };
    mockLocation();
    mockIntersectionObserver();

    mirror = await makeMirrorDesktop({
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

async function makeContext() {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        request: {
            cookie,
        },
    });
}

afterAll(() => {
    mirror.destroy();
    window.performance.timing = timing;
});

describe('Доступность меню категорий.', () => {
    describe('По умолчанию', () => {
        let container: HTMLElement;

        beforeAll(async () => {
            await makeContext();

            const {container: widgetContainer} = await apiaryLayer.mountWidget(
                widgetPath,
                packFunction(
                    nodesMock => ({nodes: nodesMock}),
                    [nodes]
                )
            );

            container = widgetContainer;
        });

        it(' категорийное меню присутствует.', async () => {
            const headerElement = container.querySelector(HeaderTabs.root);
            expect(headerElement && getByText(headerElement, 'Электроника')).toBeInTheDocument();
        });

        it(' у категорийного меню указан aria-label="Категории"', async () => {
            const categoriesElement = container.querySelector('[aria-label="Категории"]');
            expect(categoriesElement && getByText(categoriesElement, 'Электроника')).toBeInTheDocument();
        });

        it(' у категорийного меню указан role="tablist", а у табов role="tab"', async () => {
            const tablistElement = container.querySelector('[role="tablist"]');
            const tabElement = tablistElement && tablistElement.querySelector('[role="tab"]');
            expect(tabElement && getByText(tabElement, 'Электроника')).toBeInTheDocument();
        });
    });
});
