import React from 'react';
import { mount } from 'enzyme';

import AbcResourceView__Field from 'b:abc-resource-view e:field m:type=depriver';

describe('AbcResourceView__Field', () => {
    it('Should render view field of type depriver', () => {
        const depriver = {
            login: 'zomb-prj-282',
            name: { ru: 'Гермес Конрад' },
            is_dismissed: false
        };

        const wrapper = mount(
            <AbcResourceView__Field
                key="field-type-depriver"
                type="depriver"
                depriver={depriver}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
