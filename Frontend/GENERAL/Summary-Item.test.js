import React from 'react';
import { render } from 'enzyme';

import { SummaryItem } from './Summary-Item';

describe('SummaryItem', () => {
    it('Should render SummaryItem without clear button', () => {
        const wrapper = render(
            <SummaryItem
                label="Фильтр"
                value={['Значение фильтра', 5]}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('Should render SummaryItem with string value', () => {
        const wrapper = render(
            <SummaryItem
                label="Фильтр"
                value="Значение фильтра"
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
