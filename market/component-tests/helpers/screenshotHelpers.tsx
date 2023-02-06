import crypto from 'crypto';
import React from 'react';
import {render as jestPuppeteerReactRender} from 'jest-puppeteer-react';

import type {MatchImageSnapshotOptions} from 'jest-image-snapshot';
import type {ScreenshotOptions, ElementHandle, ConsoleMessage, Page} from 'puppeteer';
import type {JestPuppeteerReactRenderOptions} from 'jest-puppeteer-react';

import {currentTestData, test as jestTest, TestParams} from './componentsHelpers';

/**
 * Тайпинги и описание для хелперов находятся в screenshot.d.ts
 * При добавлении нового хелпера или правки текущего
 * нужно не забыть поправить типы в тайпингах и экпортировать его
 * в ./screenshotGlobal
 */

export {describe} from './componentsHelpers';

export const mobileViewport = {viewport: {width: 400, height: 750}};
export const desktopViewport = {viewport: {width: 1400, height: 1200}};

export const increasedThresholdConfig: MatchImageSnapshotOptions = {
    comparisonMethod: 'ssim',
    failureThreshold: 0.01,
    failureThresholdType: 'percent',
};

function logConsole(msg: ConsoleMessage) {
    // игнор ошибок типа Refused to apply style from 'http://localhost:1111/styles/highchartsAnim.css' because its MIME type ('text/html') is not a supported stylesheet MIME type, and strict MIME checking is enabled.
    if (msg.text().includes('Refused to apply style from')) {
        return;
    }
    process.stdout.write(`[${currentTestData.id}] ${msg.text()}\n`);
}

export function test(testParams: TestParams, fn: () => any) {
    if (process.env.SCREENSHOT_DEBUG) {
        page.off('console', logConsole);
        page.on('console', logConsole);
    }
    return jestTest(testParams, fn);
}

export async function setTransparentBackground(): Promise<void> {
    await page.evaluate(() => {
        document.body.style.background = 'transparent';
    });
}

export async function getElementTankerDictionary(selector?: string): Promise<Record<string, string>> {
    if (!process?.env?.I18N_DICT) {
        throw Error('Run test with "npm run test:screenshot:i18n:generate-dict"');
    }
    const tankerDict = JSON.parse(process.env.I18N_DICT) as Record<string, string>;
    // eslint-disable-next-line no-shadow
    const keys = await page.evaluate((selector: string) => {
        const tankerKeyRE = /\w[\w.-]+[:][\w.-]+\w/g;
        // eslint-disable-next-line no-shadow
        const keys = new Set<string>();
        const container = document.querySelector(selector);
        container?.querySelectorAll('*').forEach(element => {
            if (element.textContent && !element.childElementCount && element.textContent.match(tankerKeyRE)) {
                element.textContent.match(tankerKeyRE)?.forEach(match => {
                    keys.add(match);
                });
            }
        });
        return Array.from(keys);
    }, selector || 'body');
    return keys.reduce((dict, key) => (tankerDict[key] !== undefined ? {...dict, [key]: tankerDict[key]} : dict), {});
}

// eslint-disable-next-line default-param-last
export async function makeScreenshot(options: ScreenshotOptions = {}, element?: ElementHandle | null): Promise<Buffer> {
    let screenshot: Buffer;
    if (element) {
        screenshot = (await element.screenshot(options)) as Buffer;
    } else {
        screenshot = (await page.screenshot(options)) as Buffer;
    }

    const {currentTestName} = expect.getState();

    if (global.reporter) {
        global.reporter.addAttachment(currentTestName, screenshot, 'image/png');
    }

    return screenshot;
}

export function expectToMatchImageSnapshot(screenshot: Buffer, options?: MatchImageSnapshotOptions) {
    const {currentTestName} = expect.getState();

    const testId = currentTestName.match(/marketmbi-[-.0-9]+/)?.[0];

    expect(screenshot).toMatchImageSnapshot({
        ...options,
        customSnapshotIdentifier: `${testId}-${crypto.createHash('md5').update(currentTestName).digest('hex')}`,
    });
}

export async function render(component: JSX.Element, options?: JestPuppeteerReactRenderOptions): Promise<Page> {
    const page = await jestPuppeteerReactRender(<div id="test-component-wrapper">{component}</div>, {...options});
    await page.waitForSelector('#test-component-wrapper', {visible: true});

    return page;
}
