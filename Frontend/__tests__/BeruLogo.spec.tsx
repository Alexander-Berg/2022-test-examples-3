import * as React from 'react';
import { shallow } from 'enzyme';
import { LinkLayoutDefault as Link } from '@yandex-turbo/components/Link/_layout/Link_layout_default';
import { BeruLogo } from '../BeruLogo';

const data = {
    url: 'path/to/smth',
    target: '_blank',
};

describe('BeruLogo', () => {
    it('должен отрисовываться без ошибок', () => {
        const wrapper = shallow(<BeruLogo />);

        expect(wrapper.length).toEqual(1);
    });

    it('должен правильно передавать значения компоненту Link', () => {
        const wrapper = shallow(<BeruLogo {...data} />);

        expect(wrapper.find(Link).props()).toMatchObject(data);
    });
});
