import React from 'react';
import { configure, mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import AbcResourceView__Title from 'b:abc-resource-editor e:title';

configure({ adapter: new Adapter() });

describe('AbcResourceView__Title', () => {
    it('Should render common resource editor title', () => {
        const wrapper = mount(
            <AbcResourceView__Title
                key="title"
                onCancelClick={Function.prototype}
            >
                update-title
            </AbcResourceView__Title>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
