import * as React from 'react';
import { shallow } from 'enzyme';
import { Rating } from '../Rating';

const defaultData = {
    base: 10,
    value: 5.1,
    starsCount: 10,
    cls: '',
};

const additionalData = {
    text: 'hello world',
    baseColor: 'green',
    activeColor: 'black',
    cls: 'hello-world',
};

describe('Rating component', () => {
    it('should render without crashing', () => {
        const wrapper = shallow(
            <Rating {...defaultData} />
        );
        expect(wrapper.length).toEqual(1);
    });

    it('should render correct class if provided', () => {
        const wrapper = shallow(<Rating {...defaultData} cls={additionalData.cls} />);
        expect(wrapper.find('.rating').hasClass('hello-world')).toEqual(true);
    });

    it('should render correct default text', () => {
        const wrapper = shallow(<Rating {...defaultData} />);
        expect(wrapper.find('.rating__notice').text()).toEqual('5.1из 10');
    });

    it('should render correct text if text is provided', () => {
        const wrapper = shallow(<Rating {...defaultData} text={additionalData.text} />);
        expect(wrapper.find('.rating__notice').text()).toEqual(additionalData.text);
    });

    it('should not render any colors, if wasn\'t supplied', () => {
        const wrapper = shallow(<Rating {...defaultData} />);
        expect(wrapper.find('.rating__stars').prop('style'))
            .toEqual({ color: undefined, fill: undefined });
    });

    it('should render stars colors, if supplied', () => {
        const wrapper = shallow(<Rating {...defaultData} {...additionalData} />);
        expect(wrapper.find('.rating__stars').prop('style'))
            .toEqual({ color: additionalData.activeColor, fill: additionalData.baseColor });
    });
});
