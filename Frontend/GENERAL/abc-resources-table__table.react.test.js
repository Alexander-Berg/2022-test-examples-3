import React from 'react';
import { mount } from 'enzyme';

import AbcResourcesTable__table from 'b:abc-resources-table e:table';

describe('AbcResourcesTable__table', () => {
    it('Should render &__table without entities', () => {
        const ordering = { desc: false, name: '' };

        const wrapper = mount(
            <AbcResourcesTable__table
                entities={undefined}
                key="table"
                ordering={ordering}
                onTrClick={Function.prototype}
                onResourceEditOpen={Function.prototype}
                onSortClick={Function.prototype}
                onApproveClick={Function.prototype}
                onProvideClick={Function.prototype}
                onRejectClick={Function.prototype}
                onRemoveClick={Function.prototype}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
