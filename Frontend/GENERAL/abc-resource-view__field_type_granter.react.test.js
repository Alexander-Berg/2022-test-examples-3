import React from 'react';
import { mount } from 'enzyme';

import AbcResourceView__Field from 'b:abc-resource-view e:field m:type=granter';

describe('AbcResourceView__Field', () => {
    it('Should render view field of type granter', () => {
        const granter = {
            login: 'zomb-prj-282',
            name: { ru: 'Гермес Конрад' },
            is_dismissed: false
        };

        const wrapper = mount(
            <AbcResourceView__Field
                key="field-type-granter"
                type="granter"
                granter={granter}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
