import * as React from 'react';
import { shallow } from 'enzyme';
import { NewsStoryImageBlock as NewsStoryImage } from '../NewsStoryImage';

const visibilityRootRef = React.createRef() as React.RefObject<HTMLDivElement>;

const defaultData = {
    src: 'https://avatars.mds.yandex.net/get-ynews/51947/51bdff670896bc4bec59f84029bd80be/428x230',
    alt: 'Модные шляпы',
    shouldLoad: false,
    /* tslint:disable:no-empty */
    onLoadEnd: () => {},
    isVisible: false,
    isIntersecting: false,
    /* tslint:disable:no-empty */
    unobserve: () => {},
    visibilityRootRef,
};

const additionalData = {
    rounded: true,
    width: 428,
    height: 230,
    className: 'mixed-class',
};

describe('NewsStoryImage component', () => {
    it('should render without crashing', () => {
        const wrapper = shallow(
            <NewsStoryImage {...defaultData} />
        );
        expect(wrapper.length).toEqual(1);
    });

    it('should render correct class if provided', () => {
        const wrapper = shallow(<NewsStoryImage {...defaultData} {...additionalData} />);
        expect(wrapper.find('.news-story-image').hasClass('mixed-class')).toEqual(true);
    });

    it('should render rounded corners if rounded is true', () => {
        const wrapper = shallow(<NewsStoryImage {...defaultData} {...additionalData} />);
        expect(wrapper.find('.news-story-image').at(0).hasClass('news-story-image_rounded')).toEqual(true);
    });
});
