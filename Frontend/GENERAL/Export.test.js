import React from 'react';
import { render } from 'enzyme';

import { Export } from './Export';

const permissions = {
    can_export: true,
};

describe('Export', () => {
    it('Should render export block', () => {
        const wrapper = render(
            <Export
                urlParameters="state__in=develop%2Csupported&is_suspicious=false&has_external_members=false&root=0"
                permissions={{
                    ...permissions,
                    can_export: false,
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
