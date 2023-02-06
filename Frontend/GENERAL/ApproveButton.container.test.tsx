import React from 'react';
import { mount } from 'enzyme';

import { _Internal_ApproveButtonContainer } from './ApproveButton.container';
import { EDataScope } from '~/src/abc/react/redux/types';

describe('ApproveButtonContainer', () => {
    it('Should react on approve button click', () => {
        const approveDutyShift = jest.fn();

        const wrapper = mount(
            <_Internal_ApproveButtonContainer
                scope={EDataScope.Calendar}
                dutyShiftId={42}
                login="john.doe"
                setApproveDutyShift={approveDutyShift}
            />,
        );

        wrapper.find('.button2').simulate('click');
        expect(approveDutyShift).toHaveBeenCalledWith(42, true, 'john.doe');
        wrapper.unmount();
    });
});
