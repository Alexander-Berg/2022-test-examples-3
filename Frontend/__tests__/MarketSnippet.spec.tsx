import * as React from 'react';
import { shallow } from 'enzyme';
import { MarketSnippet } from '../MarketSnippet';
import dataStub from '../dataStub';

describe('компонент MarketSnippet', () => {
    it('должен отрендериться без падения', () => {
        const wrapper = shallow(<MarketSnippet {...dataStub} />);
        expect(wrapper.length).toEqual(1);
    });
});
