import * as React from 'react';
import 'jest';
import { shallow } from 'enzyme';
import { ProductsControls } from '../ProductsControls';

describe('ProductsControls', () => {
    it('Рендерится без ошибок', () => {
        shallow(<ProductsControls
            sort={{
                current: '1',
                types: [
                    {
                        type: '1',
                        url: '#1',
                        text: 'По порядку',
                    },
                    {
                        type: '2',
                        url: '#2',
                        text: 'По возрастанию',
                    },
                    {
                        type: '1',
                        url: '#1',
                        text: 'По убыванию',
                    },
                ],
            }}
            filters={{
                applied: true,
                url: '#',
            }}
            localization={{
                filters: 'Фильтры',
            }}
        />);
    });
});
