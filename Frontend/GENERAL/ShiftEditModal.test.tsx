import React from 'react';
import { shallow } from 'enzyme';
import { ShiftEditModal } from './ShiftEditModal';
import { EDataScope } from '~/src/abc/react/redux/types';

describe('Should render shift edit modal', () => {
    it('when modal is closed', () => {
        const wrapper = shallow(
            <ShiftEditModal
                shiftEditId={null}
                onSave={() => null}
                onCancel={() => null}
                onCrossClick={() => null}
                scope={EDataScope.Calendar}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('when modal is open', () => {
        const wrapper = shallow(
            <ShiftEditModal
                shiftEditId={2}
                onSave={() => null}
                onCancel={() => null}
                onCrossClick={() => null}
                scope={EDataScope.Calendar}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
