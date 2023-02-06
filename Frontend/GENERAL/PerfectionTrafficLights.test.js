import React from 'react';
import { shallow } from 'enzyme';

import { PerfectionTrafficLights } from './PerfectionTrafficLights';

describe('PerfectionModal', () => {
    it('Should render with parameters', () => {
        const wrapper = shallow(
            <PerfectionTrafficLights
                trafficLights={[
                    {
                        group: {
                            id: 1,
                            name: { ru: 'Структура' },
                        },
                        issuesCount: 5,
                        level: 'warning',
                    },
                    {
                        group: {
                            id: 2,
                            name: { ru: 'Команда' },
                        },
                        issuesCount: 0,
                        level: 'ok',
                    },
                    {
                        group: {
                            id: 3,
                            name: { ru: 'Дежурства' },
                        },
                        issuesCount: 1,
                        level: 'ok',
                    },
                    {
                        group: {
                            id: 4,
                            name: { ru: 'Железо' },
                        },
                        issuesCount: 12,
                        level: 'critical',
                    },
                    {
                        group: {
                            id: 5,
                            name: { ru: 'Прочее' },
                        },
                        issuesCount: 4,
                        level: 'warning',
                    },
                ]}
                isTooltipOpen
                onHover={jest.fn()}
                onUnhover={jest.fn()}
                onClick={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
