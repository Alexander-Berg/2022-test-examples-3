import React from 'react';
import { render } from 'enzyme';

import { PerfectionProgressBar } from './PerfectionProgressBar';
import { LEVEL_CRITICAL, LEVEL_OK, LEVEL_WARNING } from '../../redux/Perfection.actions';

const zones = [
    { level: LEVEL_CRITICAL, width: 33 },
    { level: LEVEL_WARNING, width: 33 },
    { level: LEVEL_OK, width: 34 },
];

describe('PerfectionProgressBar', () => {
    describe('with current level', () => {
        it('ok', () => {
            const wrapper = render(
                <PerfectionProgressBar
                    currentLevel="ok"
                    currentWeight={75}
                    zones={zones}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('warning', () => {
            const wrapper = render(
                <PerfectionProgressBar
                    currentLevel="warning"
                    currentWeight={66}
                    zones={zones}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('critical', () => {
            const wrapper = render(
                <PerfectionProgressBar
                    currentLevel="critical"
                    currentWeight={33}
                    zones={zones}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });
    });

    describe('with two zones', () => {
        it('critical and warning', () => {
            const wrapper = render(
                <PerfectionProgressBar
                    currentLevel="critical"
                    currentWeight={40}
                    zones={[
                        { level: LEVEL_CRITICAL, width: 50 },
                        { level: LEVEL_WARNING, width: 50 },
                    ]}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('warning and ok', () => {
            const wrapper = render(
                <PerfectionProgressBar
                    currentLevel="ok"
                    currentWeight={70}
                    zones={[
                        { level: LEVEL_WARNING, width: 50 },
                        { level: LEVEL_OK, width: 50 },
                    ]}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });
    });
});
