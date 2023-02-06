import React from 'react';
import { render } from 'enzyme';

import { Department } from './Department';

jest.mock('../../../../../../common/components/Preset/Preset');

describe('Should render Department Filter', () => {
    it('default', () => {
        const wrapper = render(
            <Department
                filterValues={[1, 2, 3]}
                onChange={() => {}}
                currentDepartment={10}
                onSuggestAdd={() => {}}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
