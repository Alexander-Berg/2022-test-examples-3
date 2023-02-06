import {CoverageEntry, ResourceType, Page} from 'puppeteer';

import {TTestCallback} from '../Runner/IRunner';
import {CustomCoverage} from '../CustomCoverage/CustomCoverage';
import {ICoveredPage, IResource} from './ITest';

export class BaseTest {
    static defaultTypes = {
        image: true,
        font: true,
        document: false,
        stylesheet: false,
        media: false,
        script: false,
        texttrack: false,
        xhr: false,
        fetch: false,
        eventsource: false,
        websocket: false,
        manifest: false,
        other: false,
        signedexchange: false,
        ping: false,
        cspviolationreport: false,
        preflight: false,
    };

    static puppeteerCoverageEntryToResource(
        coverage: CoverageEntry,
        type: ResourceType,
    ): IResource {
        return {
            resourceUri: coverage.url,
            resourceSize: Buffer.from(coverage.text).length,
            resourceType: type,
        };
    }

    protected name: string;
    protected uri: string;
    protected callback?: TTestCallback;

    constructor(name: string, uri: string, callback?: TTestCallback) {
        this.name = name;
        this.uri = uri;
        this.callback = callback;
    }

    async run(page: Page): Promise<ICoveredPage> {
        if (this.callback) {
            await this.callback(page);
        }

        return this.getStaticSize(page);
    }

    private async getStaticSize(page: Page): Promise<ICoveredPage> {
        // Это позволяет сбросить coverage, если в тесте происходила навигация.
        // Пока что на правах костыля
        await page.reload();

        const customCoverage = new CustomCoverage(page);

        await Promise.all([
            page.coverage.startJSCoverage(),
            page.coverage.startCSSCoverage(),
            customCoverage.startResourcesCoverage(BaseTest.defaultTypes),
        ]);

        await page.goto(this.uri);

        const [jsCoverage, cssCoverage, resourcesCoverage] = await Promise.all([
            page.coverage.stopJSCoverage(),
            page.coverage.stopCSSCoverage(),
            customCoverage.stopResourcesCoverage(),
        ]);

        const js = jsCoverage.reduce((all, next) => {
            all.push(BaseTest.puppeteerCoverageEntryToResource(next, 'script'));

            return all;
        }, [] as IResource[]);

        const css = cssCoverage.reduce((all, next) => {
            all.push(
                BaseTest.puppeteerCoverageEntryToResource(next, 'stylesheet'),
            );

            return all;
        }, [] as IResource[]);

        return {
            pageName: this.name,
            pageUri: this.uri,
            resources: [...js, ...css, ...resourcesCoverage],
        };
    }
}
