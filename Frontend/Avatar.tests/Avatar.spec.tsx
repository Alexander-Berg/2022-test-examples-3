import * as React from 'react';
import { shallow } from 'enzyme';
import { AvatarComponent as Avatar } from '../Avatar';
import { IProps } from '../Avatar.types';

describe('Avatar', () => {
    let props: IProps;

    beforeEach(() => {
        props = {
            avatar: 'link-to-avatar',
            fullName: 'Full Name',
            tld: 'kz',
        };
    });

    it('renders without crashing', () => {
        shallow(<Avatar { ...props } />);
    });

    it('генерирует корректную ссылку на Паспорт (kz)', () => {
        const CORRECT_PASSPORT_URL = 'https://passport.yandex.kz/profile';
        const wrapper = shallow(<Avatar { ...props } />);

        expect(wrapper.find('.turbo-avatar__link').exists()).toEqual(true);
        expect(wrapper.find('.turbo-avatar__link').props().href)
            .toEqual(CORRECT_PASSPORT_URL);
    });

    it('генерирует корректную ссылку на Паспорт (ua)', () => {
        const CORRECT_PASSPORT_URL = 'https://passport.yandex.ua/profile';
        props.tld = 'ua';
        const wrapper = shallow(<Avatar { ...props } />);

        expect(wrapper.find('.turbo-avatar__link').exists()).toEqual(true);
        expect(wrapper.find('.turbo-avatar__link').props().href)
            .toEqual(CORRECT_PASSPORT_URL);
    });

    it('подставляет ru в ссылку на паспорт, если tld не передан', () => {
        const CORRECT_PASSPORT_URL = 'https://passport.yandex.ru/profile';
        delete props.tld;
        const wrapper = shallow(<Avatar { ...props } />);

        expect(wrapper.find('.turbo-avatar__link').exists()).toEqual(true);
        expect(wrapper.find('.turbo-avatar__link').props().href)
            .toEqual(CORRECT_PASSPORT_URL);
    });
});
