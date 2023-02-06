import * as React from 'react';
import 'jest';
import { shallow } from 'enzyme';
import { EcomSecureTransactionNotice } from '../EcomSecureTransactionNotice';

describe('Компонент EcomSecureTransactionNotice', () => {
    it('renders without crashing', () => {
        shallow(<EcomSecureTransactionNotice />);
    });
});
