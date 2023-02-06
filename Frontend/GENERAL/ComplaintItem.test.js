import React from 'react';
import { render } from 'enzyme';

import ComplaintItem from './ComplaintItem';

describe('Should render', () => {
    it('default ComplaintModal', () => {
        const wrapper = render(
            <ComplaintItem
                className={'Block-Element'}
                complaint={{
                    message: 'Message',
                    createdAt: new Date('2019-09-17T00:15:16Z'),
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
