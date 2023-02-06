import React from 'react';
import { mount } from 'enzyme';

import AbcResourceView__Field from 'b:abc-resource-view e:field m:type=consumer-approver';

describe('AbcResourceView__Field', () => {
    it('Should render view field of type consumer-approver', () => {
        const approver = {
            login: 'zomb-prj-282',
            name: { ru: 'Гермес Конрад' },
            is_dismissed: false
        };

        const wrapper = mount(
            <AbcResourceView__Field
                key="field-type-approver"
                type="consumer-approver"
                approver={approver}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
