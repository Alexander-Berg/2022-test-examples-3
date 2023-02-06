import * as React from 'react';
import { shallow } from 'enzyme';
import { NewsAuthorPresenter as NewsAuthor } from '../NewsAuthor';

const defaultData = {
    avatarSrc: '//avatars.mds.yandex.net/get-entity_search/44099/103497390/S120x120Face',
    displayName: 'Марио Фигейра Фернандес',
    title: 'Российский футболист',
};

const additionalData = {
    size: 's',
    className: 'mixed-class',
};

describe('NewsAuthor component', () => {
    it('should render without crashing', () => {
        const wrapper = shallow(
            <NewsAuthor {...defaultData} />
        );
        expect(wrapper.length).toEqual(1);
    });

    it('should render correct class if provided', () => {
        const wrapper = shallow(<NewsAuthor {...defaultData} {...additionalData} />);
        expect(wrapper.find('.news-author').hasClass('mixed-class')).toEqual(true);
    });

    it('should render correct default size', () => {
        const wrapper = shallow(<NewsAuthor {...defaultData} />);
        expect(wrapper.find('.news-author').hasClass('news-author_size_m')).toEqual(true);
    });

    it('should render correct size if size is provided', () => {
        const wrapper = shallow(<NewsAuthor {...defaultData} {...additionalData} />);
        expect(wrapper.find('.news-author').hasClass('news-author_size_s')).toEqual(true);
    });
});
