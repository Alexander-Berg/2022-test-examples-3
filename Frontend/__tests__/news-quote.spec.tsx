import * as React from 'react';
import { shallow } from 'enzyme';
import { NewsQuote } from '../NewsQuote';

const defaultData = {
    children: 'Quote text',
    className: undefined,
};

const additionalData = {
    children: (
        'Это значит три титула премьер-лиги,' +
        'я выиграл их больше, чем тренеры 19 остальных' +
        'клубов вместе взятые. Так что уважение,' +
        'уважение, уважение.'
    ),
    className: 'my-test-class',
};

describe('NewsQuote component', () => {
    it('should render without crashing', () => {
        const wrapper = shallow(
            <NewsQuote {...defaultData} />
        );
        expect(wrapper.length).toEqual(1);
    });

    it('should render correct class if provided', () => {
        const wrapper = shallow(<NewsQuote {...defaultData} className={additionalData.className} />);
        expect(wrapper.find('.news-quote').hasClass('my-test-class')).toEqual(true);
    });

    it('should render correct default text', () => {
        const wrapper = shallow(<NewsQuote {...defaultData} />);
        expect(wrapper.find('.news-quote').text()).toEqual('Quote text');
    });

    it('should render correct text if text is provided', () => {
        const wrapper = shallow(<NewsQuote {...defaultData} {...additionalData} />);
        expect(wrapper.find('.news-quote').text()).toEqual(additionalData.children);
    });
});
