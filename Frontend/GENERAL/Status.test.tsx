import React from 'react';
import { composeU } from '@bem-react/core';
import { shallow } from 'enzyme';

import { Status as StatusBase } from '~/src/features/Duty/components/Status/Status';
import { withStatusModeApproved } from '~/src/features/Duty/components/Status/_mode/withModeApproved';
import { withStatusModePending } from '~/src/features/Duty/components/Status/_mode/withModePending';
const Status = composeU(withStatusModeApproved, withStatusModePending)(StatusBase);

describe('Should render Status', () => {
    it('In pending status', () => {
        const wrapper = shallow(
            <Status mode="pending" />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('In approved status', () => {
        const wrapper = shallow(
            <Status mode="approved" />,

        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
