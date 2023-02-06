import React from 'react';
import renderer from 'react-test-renderer';

import {
    TimeUnits,
} from '../core';
import {
    cnTimeLeft,
    TimeLeftDesktop,
    TimeLeftTouchPhone,
} from '.';

describe('TimeLeft', () => {
    test('cnTimeLeft', () => {
        expect(cnTimeLeft()).toBe('YpcTimeLeft');
    });

    for (const [platform, TimeLeft] of Object.entries({
        desktop: TimeLeftDesktop,
        'touch-phone': TimeLeftTouchPhone,
    })) {
        describe(platform, () => {
            test('className', () => {
                const tree = renderer
                    .create(
                        <TimeLeft
                            timeStamp={300}
                            className={'Test'}
                        />,
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            describe('timeStamp & timeUnits', () => {
                test('zero seconds', () => {
                    const tree = renderer
                        .create(
                            <TimeLeft
                                timeStamp={0}
                                timeUnits={TimeUnits.Second}
                            />,
                        )
                        .toJSON();

                    expect(tree).toMatchSnapshot();
                });

                test('minutes unit', () => {
                    const tree = renderer
                        .create(
                            <TimeLeft
                                timeStamp={25}
                                timeUnits={TimeUnits.Minute}
                            />,
                        )
                        .toJSON();

                    expect(tree).toMatchSnapshot();
                });

                test('default unit', () => {
                    const tree = renderer
                        .create(
                            <TimeLeft
                                timeStamp={300}
                            />,
                        )
                        .toJSON();

                    expect(tree).toMatchSnapshot();
                });
            });
        });
    }
});
