import React from 'react';
import { shallow } from 'enzyme';

import AbcResourcesTable__Td from 'b:abc-resources-table e:td m:type=consumer';

describe('AbcResourcesTable__Td', () => {
    it('Should render empty abc-resources-table__td_type_consumer', () => {
        const wrapper = shallow(
            <AbcResourcesTable__Td
                type="consumer"
                tags={[]}
                service={{ slug: 'foo', name: { ru: 'bar' } }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render abc-resources-table__td_type_consumer', () => {
        const wrapper = shallow(
            <AbcResourcesTable__Td
                type="consumer"
                tags={[
                    {
                        name: { ru: 'foo' }
                    }
                ]}
                service={{ slug: 'foo', name: { ru: 'bar' } }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
