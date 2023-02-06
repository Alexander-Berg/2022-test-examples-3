import MobileDirect from 'components/direct/mobile';
import React from 'react';
import { shallow } from 'enzyme';

describe('direct', () => {
    it('default', () => {
        const component = shallow(<MobileDirect />);
        expect(component).toMatchSnapshot();
    });

    it('custom blockId', () => {
        const component = shallow(<MobileDirect blockId="R-I-267060-3" />);
        expect(component).toMatchSnapshot();
    });

    it('custom cls', () => {
        const component = shallow(<MobileDirect cls="my-super-direct" />);
        expect(component).toMatchSnapshot();
    });

    it('custom blockId and cls', () => {
        const component = shallow(<MobileDirect blockId="R-I-104220-41" cls="my-super-direct" />);
        expect(component).toMatchSnapshot();
    });
});
