import React from 'react';
import { shallow } from 'enzyme';

import __AttributesActions from 'b:abc-resource-view e:attributes-actions';

describe('__AttributesActions', () => {
    it('Should render empty actions', () => {
        const wrapper = shallow(
            <__AttributesActions
                actions={[]}
                onAttributeActionClick={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });

    it('Should render all the actions', () => {
        const wrapper = shallow(
            <__AttributesActions
                actions={[
                    'recreate_secret',
                    'restore_secret',
                    'delete_old_secret',
                    'reset_password'
                ]}
                onAttributeActionClick={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });
});
