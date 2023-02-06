import * as React from 'react';
import { shallow } from 'enzyme';
import { NewsStorySource } from '../NewsStorySource';

const defaultData = {
    name: 'Wikipedia',
    className: undefined,
};

const additionalData = {
    name: 'Wikipedia Forever',
    url: 'http://wikipedia.com',
    className: 'mixed-class',
};

describe('NewsStorySource component', () => {
    it('should render without crashing', () => {
        const wrapper = shallow(
            <NewsStorySource {...defaultData} />
        );
        expect(wrapper.length).toEqual(1);
    });

    it('should render correct class if provided', () => {
        const wrapper = shallow(<NewsStorySource {...defaultData} {...additionalData} />);
        expect(wrapper.find('.news-story-source').hasClass('mixed-class')).toEqual(true);
    });

    it('should render correct default name', () => {
        const wrapper = shallow(<NewsStorySource {...defaultData} />);
        expect(wrapper.find('.news-story-source').prop('text')).toEqual('Wikipedia');
    });

    it('should render correct name if name is provided', () => {
        const wrapper = shallow(<NewsStorySource {...defaultData} {...additionalData} />);
        expect(wrapper.find('.news-story-source').prop('text')).toEqual(additionalData.name);
    });

    it('should render correct url if url is provided', () => {
        const wrapper = shallow(<NewsStorySource {...defaultData} {...additionalData} />);
        expect(wrapper.find('.news-story-source').prop('url')).toEqual(additionalData.url);
    });
});
