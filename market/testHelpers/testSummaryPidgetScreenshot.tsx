import React from 'react';
import type {MatchImageSnapshotOptions} from 'jest-image-snapshot';
import {Registry, Button, RatingMeter} from '@yandex-levitan/b2b';

type TestSummaryPidgetScreenshotOptions = {
    width: number;
    matchSnapshotOptions?: MatchImageSnapshotOptions;
};

export const testSummaryPidgetScreenshot = async (pidget: JSX.Element, options: TestSummaryPidgetScreenshotOptions) => {
    const {width, matchSnapshotOptions} = options;

    const outlinedPidget = (
        <div id="pidget-outline" style={{background: '#ededed', padding: 16, width}}>
            <Registry.RegistryProvider
                {...{
                    [String(Button)]: Button,
                    [String(RatingMeter)]: RatingMeter,
                }}
            >
                {pidget}
            </Registry.RegistryProvider>
        </div>
    );
    const page = await shot.render(outlinedPidget, shot.desktopViewport);

    const screenshot = await shot.makeScreenshot({}, await page.$('#pidget-outline'));
    shot.expectToMatchImageSnapshot(screenshot, matchSnapshotOptions);
};
