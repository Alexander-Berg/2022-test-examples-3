import React from 'react';
import { render } from 'enzyme';

import { Owner } from './Owner';

jest.mock('../../../../../../common/components/Preset/Preset');

const { BEM_LANG } = process.env;

describe('Should render Owner Filter', () => {
    it('default', () => {
        const wrapper = render(
            <Owner
                filterValues={['user1', 'user2']}
                onChange={() => {}}
                currentUser={{
                    login: 'user0',
                    name: { [BEM_LANG]: 'Юзер0' },
                }}
                onSuggestAdd={() => {}}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
