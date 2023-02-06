import * as React from 'react';
import { shallow } from 'enzyme';
import { ThumbPosition, FeedItem, IProps as IFeedItemProps, FeedItemText } from '@yandex-turbo/components/FeedItem/FeedItem';
import { ImageSimple } from '@yandex-turbo/components/ImageSimple/ImageSimple';
import { FeedPresenter as Feed, LayoutType, defaultTitle } from '../Feed';

const defaultItemData: IFeedItemProps = {
    href: 'http://google.com',
    title: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor',
    image: <ImageSimple src="https://picsum.photos/300/300/?random" />,
    thumbPosition: ThumbPosition.left,
    description: 'Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.',
};

describe('Feed component', () => {
    it('should render without crashing', () => {
        const wrapper = shallow(
            <Feed title={'Title'}>
                <FeedItem {...defaultItemData} />
                <FeedItem {...defaultItemData} />
                <FeedItem {...defaultItemData} />
            </Feed>
        );
        expect(wrapper.length).toEqual(1);
    });

    it('should render without props', () => {
        const wrapper = shallow(<Feed />);
        expect(wrapper.length).toEqual(1);
    });

    it('should render correct title', () => {
        const wrapper = shallow(<Feed title={'Title'} />);
        expect(wrapper.find('.feed__title').text()).toEqual('Title');
    });

    it('should render correct title if title prop is emty', () => {
        const wrapper = shallow(<Feed title={''} />);
        expect(wrapper.find('.feed__title').text()).toEqual(defaultTitle);
    });

    it('should render correct horizontal layout', () => {
        const wrapper = shallow(<Feed title={'Title'} layout={LayoutType.horizontal} />);
        expect(wrapper.hasClass('feed_layout_horizontal')).toEqual(true);
    });

    it('should render correct items count', () => {
        const wrapper = shallow(
            <Feed title={'Title'}>
                <FeedItem {...defaultItemData} />
                <FeedItem {...defaultItemData} />
                <FeedItem {...defaultItemData} />
            </Feed>
        );
        expect(wrapper.find(FeedItem).length).toEqual(3);
    });

    it('should set correct props to item', () => {
        const wrapper = shallow(
            <Feed title={'Title'}>
                <FeedItem {...defaultItemData} />
            </Feed>
        );
        const props = wrapper.find(FeedItem).props();

        expect(props.href).toEqual(defaultItemData.href);
        expect(props.description).toEqual(defaultItemData.description);
        expect(props.thumbPosition).toEqual(defaultItemData.thumbPosition);
        expect(props.title).toEqual(defaultItemData.title);
    });

    it('should render items', () => {
        const wrapper = shallow(
            <Feed title={'Title'}>
                <FeedItem {...defaultItemData} />
            </Feed>
        );
        expect(wrapper.find(FeedItem).render().length).toEqual(1);
    });

    it('should correct render item title', () => {
        const wrapper = shallow(<FeedItemText {...defaultItemData} />);
        expect(wrapper.find('.feed-item__title').text()).toEqual(defaultItemData.title);
    });

    it('should correct render item description if thumb position is "top"', () => {
        const customData = Object.assign({}, defaultItemData, { thumbPosition: 'top' });
        const wrapper = shallow(<FeedItemText {...customData} />);
        expect(wrapper.find('.feed-item__description').length).toEqual(1);
        expect(wrapper.find('.feed-item__description').text()).toEqual(defaultItemData.description);
    });

    it('should not render item description if layout is "horizontal"', () => {
        const customData = Object.assign({}, defaultItemData, { thumbPosition: 'top' });
        const wrapper = shallow(
            <Feed layout={LayoutType.horizontal}>
                <FeedItem {...customData} />
            </Feed>
        );
        expect(wrapper.find(FeedItem).render().find('.feed-item__description').length).toEqual(0);
    });
});
