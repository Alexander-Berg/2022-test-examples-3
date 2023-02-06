import React from 'react';
import { shallow } from 'enzyme';

import AbcSortableList__Item from 'b:abc-sortable-list e:item';

const index = 2;
const id = `test_${index}`;
const isDisabled = true;
const item = 'content';

describe('AbcSortableList__Item', () => {
    it('Should render disabled sortable list', () => {
        const itemRender = jest.fn();

        const wrapper = shallow(
            <AbcSortableList__Item
                id={id}
                index={index}
                isDisabled={isDisabled}
                item={item}
                itemRender={itemRender}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
