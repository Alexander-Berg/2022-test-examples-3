import * as React from 'react';
import 'jest';
import { shallow } from 'enzyme';
import { EcomSecureTransactionBadge } from '../EcomSecureTransactionBadge';

describe('Компонент EcomSecureTransactionBadge', () => {
    it('рисуется без ошибок', () => {
        shallow(<EcomSecureTransactionBadge />);
    });
});
