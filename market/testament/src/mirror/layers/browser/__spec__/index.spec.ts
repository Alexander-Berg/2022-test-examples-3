import {getDocument, queries, waitFor} from 'playwright-testing-library';
import {Page} from 'playwright';
import Runtime from '@yandex-market/apiary/client/runtime';

import Mirror from '../../..';
import JestLayer from '../../jest';
import MandrelLayer from '../../mandrel';
import TestamentBrowserLayer from '..';
import ApiaryLayer from '../../apiary';
import KadavrLayer from '../../kadavr';
import EsbuildWidgetLayer from '../../widgetBuilder/esbuild';

declare const testamentApiaryRuntime: Runtime;

const mirror = new Mirror();
const jestLayer = new JestLayer(__filename, jest);
const mandrelLayer = new MandrelLayer();
const testamentBrowserLayer = new TestamentBrowserLayer({
    useWidgetBuilderLayerClass: EsbuildWidgetLayer,
});
const apiaryLayer = new ApiaryLayer();
const esbuildWidgetLayer = new EsbuildWidgetLayer();
const kadavrLayer = new KadavrLayer({host: 'localhost', port: 15566});

// eslint-disable-next-line @typescript-eslint/no-var-requires
const {createQuestion} = require('@yandex-market/kadavr/mocks/PersQa/helpers');

const {getByRole, queryAllByRole} = queries;

const bootstrapLayers = async () => {
    await mandrelLayer.initContext();
};

beforeAll(async () => {
    await mirror.registerRuntime(jestLayer);
    await mirror.registerLayer(mandrelLayer);
    await mirror.registerLayer(apiaryLayer);
    await mirror.registerLayer(testamentBrowserLayer);
    await mirror.registerLayer(kadavrLayer);
    await mirror.registerLayer(esbuildWidgetLayer);
});

beforeEach(async () => {
    await bootstrapLayers();
});

afterAll(() => mirror.destroy());

describe('simple widget', () => {
    describe('mountWidget', () => {
        let page: Page;

        beforeEach(async () => {
            page = await testamentBrowserLayer.mountWidget(
                '../../../../platform/browser/__tests__/MyWidget',
                {
                    items: [1, 2],
                },
            );
        });

        afterEach(async () => {
            await page.close();
        });

        test('should clear data', async () => {
            const container = await getDocument(page);

            expect(await queryAllByRole(container, 'item')).toHaveLength(2);
            await (await getByRole(container, 'my-button')).click();
            expect(await queryAllByRole(container, 'item')).toHaveLength(0);
        });

        test('should count touches', async () => {
            const container = await getDocument(page);

            expect(
                await (await getByRole(container, 'touches')).textContent(),
            ).toBe('0');
            await (await getByRole(container, 'touch-button')).click();
            expect(
                await (await getByRole(container, 'touches')).textContent(),
            ).toBe('1');
        });

        test('should expose runtime', async () => {
            const container = await getDocument(page);

            expect(await queryAllByRole(container, 'item')).toHaveLength(2);
            await container.evaluate(() => {
                testamentApiaryRuntime
                    ._selectStoreByWidgetSource(null)
                    .dispatch({type: '#ONE', meta: {widgetId: '/'}});
            });
            expect(await queryAllByRole(container, 'item')).toHaveLength(0);
        });
    });
});

describe('network widget', () => {
    describe('mountWidget', () => {
        let page: Page;

        beforeEach(async () => {
            page = await testamentBrowserLayer.mountWidget(
                '../../../../platform/browser/__tests__/MyNetworkWidget',
                {
                    items: [1, 2],
                },
            );
        });

        test('should init', async () => {
            const container = await getDocument(page);
            const content = await getByRole(container, 'content');
            const textContent = await content?.textContent();

            expect(textContent).toBe('initial');
        });

        test('should call kadavr layer mock (no set state)', async () => {
            const container = await getDocument(page);

            await (await getByRole(container, 'my-button')).click();

            await waitFor(async () => {
                const content = await getByRole(container, 'content');
                const textContent = await content?.textContent();
                expect(textContent).toContain('"data":[]');
            });
        });

        test('should call kadavr layer mock (set state)', async () => {
            const container = await getDocument(page);

            const question = createQuestion();

            await kadavrLayer.setState('storage.modelQuestions', [question]);

            await (await getByRole(container, 'my-button')).click();

            await waitFor(async () => {
                const content = await getByRole(container, 'content');
                const textContent = await content?.textContent();
                expect(textContent).toContain(`"text":"${question.text}"`);
            });
        });

        test('should work with browser reload and resize', async () => {
            let container = await getDocument(page);

            const question = createQuestion();

            await kadavrLayer.setState('storage.modelQuestions', [question]);

            await page.reload({waitUntil: 'domcontentloaded'});

            await page.setViewportSize({width: 100, height: 100});

            container = await getDocument(page);

            await (await getByRole(container, 'my-button')).click();

            await waitFor(async () => {
                const content = await getByRole(container, 'content');
                const textContent = await content?.textContent();
                expect(textContent).toContain(`"text":"${question.text}"`);
            });
        });
    });
});
