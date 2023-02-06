import * as React from 'react';
import 'jest';
import { shallow } from 'enzyme';
import { EcomPromoBadge } from '../EcomPromoBadge';

describe('Компонент EcomPromoBadge', () => {
    it('Рендерится без ошибок', () => {
        shallow(<EcomPromoBadge url="https://ya.ru" />);
    });
});
