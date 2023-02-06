import React from 'react';
import { shallow } from 'enzyme';

import AbcSortableList__EmptyContent from 'b:abc-sortable-list e:empty-content';

describe('AbcSortableList__EmptyContent', () => {
    it('Should render content of an empty list', () => {
        const wrapper = shallow(
            <AbcSortableList__EmptyContent />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
