import React from 'react';
import { shallow } from 'enzyme';

import AbcResourceViewName from 'b:abc-resource-view e:name';

describe('Render resource view name', () => {
    it('Should render common resource view name with link', () => {
        const wrapper = shallow(
            <AbcResourceViewName
                link="someLink"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render common resource view name without link', () => {
        const wrapper = shallow(
            <AbcResourceViewName />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
