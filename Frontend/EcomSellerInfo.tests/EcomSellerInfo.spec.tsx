import * as React from 'react';
import 'jest';
import { shallow } from 'enzyme';
import { EcomSellerInfo } from '../EcomSellerInfo';

describe('Компонент EcomPromoBadge', () => {
    it('рендерится без ошибок', () => {
        shallow(<EcomSellerInfo>Информация о продавце</EcomSellerInfo>);
    });
});
