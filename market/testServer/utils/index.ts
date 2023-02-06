import path from 'path';
import crypto from 'crypto';

import type {MatchImageSnapshotOptions} from 'jest-image-snapshot';
import type {Page} from 'puppeteer';

const testIds: Record<string, string> = {};
export const setTestId = (testId: string) => {
    const {currentTestName} = expect.getState();

    testIds[currentTestName] = testId;
};

export const getTestId = (): string => {
    const {currentTestName} = expect.getState();

    return testIds[currentTestName];
};

export const step = async <T>(name: string, stepFn: () => Promise<T>): Promise<T> => {
    reporter.startStep(name);
    const result = await stepFn();
    reporter.endStep();

    return result;
};

export const getImageName = (): string => {
    const {testPath, currentTestName, assertionCalls} = expect.getState();

    let uniqueId = getTestId();
    if (!uniqueId) {
        uniqueId = crypto.createHash('md5').update(`${testPath}-${currentTestName}`).digest('hex');
    }

    return `${uniqueId}-${assertionCalls}`;
};

export const expectToMatchImageSnapshot = (screenshot: Buffer | string, options?: MatchImageSnapshotOptions) => {
    expect(screenshot).toMatchImageSnapshot({
        ...options,
        customSnapshotIdentifier: getImageName(),
    });
};

export const screenshotElement = async (page: Page, selector?: string): Promise<Buffer | string> => {
    return step(`Делаем скриншот ${selector ? `элемента ${selector}` : 'страницы'}`, async () => {
        let boundingBox;
        let params;

        if (selector) {
            await page.waitForSelector(selector, {timeout: 10000});
            const element = await page.$(selector);
            boundingBox = await element?.boundingBox();
        }

        if (boundingBox) {
            const {x, y, width, height} = boundingBox;
            params = {clip: {x, y, width, height}};
        }

        const image = await page.screenshot(params);

        reporter.addAttachment(`${getImageName()}-snap`, image, 'image/png');

        return image;
    });
};
