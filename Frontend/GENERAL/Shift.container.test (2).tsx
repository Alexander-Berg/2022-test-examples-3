import React from 'react';
import { mount } from 'enzyme';

import { ShiftContainer } from './Shift.container';
import type { User } from '~/src/features/Duty2/components/Calendar/Calendar.types';

function doNothing() { }

describe('Should toggle state upon mouse events', () => {
    beforeEach(() => {
        jest.useFakeTimers();
    });

    it('With default delays', () => {
        const defaultShowDelay = 100;
        const defaultHideDelay = 300;

        const wrapper = mount(
            <ShiftContainer
                id={1}
                replacementForId={null}
                onOpenShiftEditClick={doNothing}
                showDutyDetails={doNothing}
                hideDutyDetails={doNothing}
                user={{} as User}
                onHoverChange={jest.fn()}
            />,
        );

        wrapper.simulate('mouseenter');
        jest.advanceTimersByTime(defaultShowDelay);
        expect(wrapper.state('isDetailsVisible')).toBe(true);

        wrapper.simulate('mouseleave');
        jest.advanceTimersByTime(defaultHideDelay);
        expect(wrapper.state('isDetailsVisible')).toBe(false);

        wrapper.unmount();
    });

    it('With custom delays', () => {
        const showDelay = 500;
        const hideDelay = 1000;

        const wrapper = mount(
            <ShiftContainer
                id={1}
                replacementForId={null}
                showDetailsDelay={showDelay}
                hideDetailsDelay={hideDelay}
                onOpenShiftEditClick={doNothing}
                showDutyDetails={doNothing}
                hideDutyDetails={doNothing}
                user={{} as User}
                onHoverChange={jest.fn()}
            />,
        );

        wrapper.simulate('mouseenter');
        jest.advanceTimersByTime(showDelay);
        expect(wrapper.state('isDetailsVisible')).toBe(true);

        wrapper.simulate('mouseleave');
        jest.advanceTimersByTime(hideDelay);
        expect(wrapper.state('isDetailsVisible')).toBe(false);

        wrapper.unmount();
    });
});
