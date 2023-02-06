import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruSkuFilter } from '../BeruSkuFilter';

describe('компонент BeruSkuFilter', () => {
    const props = {
        title: 'Цвет',
        values: [
            {
                url: '/turbo?text=https://beru.ru/product/123',
                value: 'красный',
                checked: true,
                image: '//avatars.mds.yandex.net/get-mpic/906397/img_id5744262038440462986.jpeg/orig',
            },
        ],
    };

    it('должен отрендериться без падения', () => {
        const wrapper = shallow(<BeruSkuFilter {...props} />);

        expect(wrapper.length).toEqual(1);
    });
});
