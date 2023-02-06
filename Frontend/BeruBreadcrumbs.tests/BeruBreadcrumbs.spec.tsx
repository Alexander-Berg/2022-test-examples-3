import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruBreadcrumbs } from '../BeruBreadcrumbs';
import { dataStub } from './datastub';

describe('BeruBreadcrumbs', () => {
    it('по умолчанию отображается без ошибок', () => {
        const wrapper = shallow(<BeruBreadcrumbs {...dataStub} />);

        expect(wrapper.hasClass('beru-breadcrumbs')).toEqual(true);
    });
});
