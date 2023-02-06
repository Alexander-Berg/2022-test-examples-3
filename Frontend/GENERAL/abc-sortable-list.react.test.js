import React from 'react';
import { shallow } from 'enzyme';

import AbcSortableList from 'b:abc-sortable-list';

const listName = 'test';
const listItems = ['one', 'two', 'three'];
const sortableListProps = {
    lists: {
        someRandomName: {
            items: [0, 1, 2]
        },
        [listName]: {
            items: listItems
        }
    }
};

describe('AbcSortableList', () => {
    it('Should render sortable list', () => {
        const itemRender = () => 'Item render goes here...';

        const wrapper = shallow(
            <AbcSortableList
                id={listName}
                sortableListProps={sortableListProps}
            >
                {itemRender}
            </AbcSortableList>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
