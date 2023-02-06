import path from 'path';

import {Page} from 'playwright';

import Layer from '../../layer';
import ApiaryLayer from '../apiary';
import JestLayer from '../jest';
import {TestamentBrowser} from '../../../platform/browser/browser/TestamentBrowser';
import PackedFunction from '../../packedFunction';
import {TestamentBrowserWorker} from './worker';
import {resolveScriptPath} from '../../../utils/relativePath';
import KadavrLayer from '../kadavr';
import {NoMockResolverError} from '../kadavr/errors/noMockResolverError';
import {NoMockRouteError} from '../kadavr/errors/noMockRouteError';
import template from '../../../platform/browser/widget/template';
import AbstractWidgetBuilderLayer from '../widgetBuilder/abstract';

export interface TestamentBrowserLayerConfig {
    useWidgetBuilderLayerClass: {
        ID: string;
    } & typeof AbstractWidgetBuilderLayer<any, any>;
}

// eslint-disable-next-line @typescript-eslint/ban-types
export default class TestamentBrowserLayer extends Layer<
    // eslint-disable-next-line @typescript-eslint/ban-types
    {},
    TestamentBrowserWorker
> {
    static readonly ID = 'browser';

    #browser?: TestamentBrowser;

    #config: TestamentBrowserLayerConfig;

    constructor(config: TestamentBrowserLayerConfig) {
        super(
            TestamentBrowserLayer.ID,
            resolveScriptPath(__filename, './worker.js'),
        );

        this.#config = config;
    }

    async init(): Promise<void> {
        await super.init();
        await this.worker.init();
        this.#browser = new TestamentBrowser(await this.worker.getWsEndpoint());
    }

    async mountWidget<TWidgetProps extends Record<any, any>>(
        pathToWidget: string,
        props?:
            | TWidgetProps
            | (() => Promise<TWidgetProps> | TWidgetProps)
            | PackedFunction<any[], TWidgetProps>,
    ): Promise<Page> {
        if (!this.#browser) {
            throw new Error(
                'Browser layer - browser not defined, probably no init call in TestamentBrowserLayer',
            );
        }

        const apiaryLayer = this.getMirror()?.getLayer<ApiaryLayer>(
            ApiaryLayer.ID,
        );
        if (!apiaryLayer) {
            throw new Error('BrowserLayer - ApiaryLayer not defined');
        }

        const jestLayer = this.getMirror()?.getLayer<JestLayer>(JestLayer.ID);
        if (!jestLayer) {
            throw new Error('BrowserLayer - JestLayer not defined');
        }

        const widgetBuildLayer = this.getMirror()?.getLayer<
            AbstractWidgetBuilderLayer<any, any>
        >(this.#config.useWidgetBuilderLayerClass.ID);
        if (!widgetBuildLayer) {
            throw new Error('BrowserLayer - WidgetBuildLayer not defined');
        }

        const testDirname = path.dirname(
            jestLayer.getTestFilename() || __filename,
        );

        const widgetFullPath = apiaryLayer.getWidgetFullPath(pathToWidget);

        const file = template(widgetFullPath);

        const {js, css} = await widgetBuildLayer.build(
            file,
            widgetFullPath,
            testDirname,
        );

        const result = await apiaryLayer.processWidget(widgetFullPath, props);

        const context = await this.#browser.getContext();

        const page = await context.newPage();
        await page.setContent(
            `<style>${css}</style>${result?.data.html ?? ''}`,
        );
        await page.evaluate(js);

        // На текущий момент у нас для виджета создается изолированный контекст с одной страницей.
        // Если кто-то в тестах вызовет руками закрытие страницы - надо подчистить и контекст.
        page.on('close', () => {
            this.#browser?.closeContext(context).catch(e => {
                console.error(
                    `Error while closing browser context ${e.message}`,
                );
            });
        });

        // На настоящий момент не нашлось способа проставить контент с вижджетом после обновления страницы так,
        // чтобы в тесте не пришлось ждать. Завязки на эвенты событий страницы приводят к race.
        // Решил пока таким способом.

        // @ts-ignore
        page.oldReload = page.reload;

        page.reload = async function (options: any) {
            // @ts-ignore
            await page.oldReload(options);
            await page.setContent(
                `<style>${css}</style>${result?.data.html ?? ''}`,
            );
            await page.evaluate(js);

            return null;
        };

        // eslint-disable-next-line consistent-return
        await page.route('**', async route => {
            const req = route.request();
            const url = new URL(req.url());
            const method = req.method();
            const body = req.postDataBuffer();
            const headers = await req.allHeaders();
            try {
                const kadavrLayer = this.getMirror()?.getLayer<KadavrLayer>(
                    KadavrLayer.ID,
                );

                const kadavrResult = await kadavrLayer?.execMock(
                    url,
                    method,
                    body,
                    headers,
                );

                return route.fulfill({
                    body: JSON.stringify(kadavrResult),
                    status: 200,
                });
            } catch (e) {
                if (
                    e instanceof Error &&
                    (e.name === NoMockResolverError.name ||
                        e.name === NoMockRouteError.name)
                ) {
                    return route.abort();
                }
                console.log(e);
            }
        });

        return page;
    }

    // eslint-disable-next-line class-methods-use-this,@typescript-eslint/ban-types
    getMethods(): {} {
        return {};
    }

    async destroy(): Promise<void> {
        await this.#browser?.destroy();
    }
}
