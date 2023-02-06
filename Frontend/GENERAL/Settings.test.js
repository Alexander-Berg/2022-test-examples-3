import React from 'react';
import { render } from 'enzyme';

import { Settings } from './Settings';

const requiredProps = {
    service: { id: 42 },
    isEdit: false,
    onScheduleChange: jest.fn(),
    onScheduleAdd: jest.fn(),
    onSubmit: jest.fn(),
    onCancel: jest.fn(),
    isSlugEditable: jest.fn(),
};

describe('Should render duty settings', () => {
    it('in default state', () => {
        const wrapper = render(<Settings {...requiredProps} />);
        expect(wrapper).toMatchSnapshot();
    });

    describe('with critical fields alert', () => {
        it('fields', () => {
            const wrapper = render(
                <Settings
                    {...requiredProps}
                    criticalFields={{
                        conditions: ['fields'],
                        consequence: 'soft',
                        fields: ['algorithm'],
                    }}
                />,
            );
            expect(wrapper).toMatchSnapshot();
        });

        it('schedule', () => {
            const wrapper = render(
                <Settings
                    {...requiredProps}
                    criticalFields={{
                        conditions: ['schedule'],
                        consequence: 'soft',
                        fields: [],
                    }}
                />,
            );
            expect(wrapper).toMatchSnapshot();
        });

        it('fields and schedule', () => {
            const wrapper = render(
                <Settings
                    {...requiredProps}
                    criticalFields={{
                        conditions: ['fields', 'schedule'],
                        consequence: 'soft',
                        fields: ['algorithm'],
                    }}
                />,
            );
            expect(wrapper).toMatchSnapshot();
        });

        it('soft consequence', () => {
            const wrapper = render(
                <Settings
                    {...requiredProps}
                    criticalFields={{
                        conditions: ['fields'],
                        consequence: 'soft',
                        fields: ['algorithm'],
                    }}
                />,
            );
            expect(wrapper).toMatchSnapshot();
        });

        it('hard consequence', () => {
            const wrapper = render(
                <Settings
                    {...requiredProps}
                    criticalFields={{
                        conditions: ['fields'],
                        consequence: 'hard',
                        fields: ['duration'],
                    }}
                />,
            );
            expect(wrapper).toMatchSnapshot();
        });

        it('single field', () => {
            const wrapper = render(
                <Settings
                    {...requiredProps}
                    criticalFields={{
                        conditions: ['fields'],
                        consequence: 'soft',
                        fields: ['algorithm'],
                    }}
                />,
            );
            expect(wrapper).toMatchSnapshot();
        });

        it('multiple fields', () => {
            const wrapper = render(
                <Settings
                    {...requiredProps}
                    criticalFields={{
                        conditions: ['fields'],
                        consequence: 'soft',
                        fields: ['algorithm', 'duration'],
                    }}
                />,
            );
            expect(wrapper).toMatchSnapshot();
        });
    });
});
