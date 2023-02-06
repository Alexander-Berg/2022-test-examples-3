import React from 'react';
import { shallow } from 'enzyme';
import { IconClose } from '../../../../static/icons/close';

import { Tags } from './Tags';

describe('UI/Tags', () => {
    it('should render default mg-light Tags', () => {
        const wrapper = shallow(<Tags theme="mg-light" name="tags" value={[]} onChange={() => null} />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render Tags with options only', () => {
        const wrapper = shallow(<Tags theme="mg-light" inputMode="options-only" name="tags" value={['string', 123]} onChange={() => null} removable canCleaned />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render Tags with long-integers mode', () => {
        const wrapper = shallow(<Tags theme="mg-light" inputMode="long-integers" name="tags" value={['11111111111111111111111', '2222222222222222222222']} onChange={() => null} />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render Tags with text mode', () => {
        const wrapper = shallow(<Tags theme="mg-light" inputMode="text" name="tags" value={['string1', 'string2', 'string3']} onChange={() => null} />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render Tags with integers mode', () => {
        const wrapper = shallow(<Tags theme="mg-light" inputMode="integers" name="tags" value={[123, 456]} onChange={() => null} />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render Tags with specified icon', () => {
        const wrapper = shallow(<Tags theme="mg-light" name="tags" value={[]} onChange={() => null} icon={IconClose} />);
        expect(wrapper).toMatchSnapshot();
    });
});
