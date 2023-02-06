import React from 'react';
import { shallow } from 'enzyme';

import AbcSortableListsContext from 'b:abc-sortable-lists-context';

const contentRender = () => 'List of lists goes here...';

describe('AbcSortableListsContext', () => {
    it('Should render context of sortable lists', () => {
        const wrapper = shallow(
            <AbcSortableListsContext
                lists={{}}
                onReorder={() => {}}
            >
                {contentRender}
            </AbcSortableListsContext>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});

/**
 * Next go tests of the logic of the callback given to the library.
 */

describe('Should not call `onReorder` from `_onDragEnd`', () => {
    const onReorder = jest.fn();

    const listName = 'test';
    const lists = {
        [listName]: {
            items: [
                'one'
            ]
        }
    };

    let wrapper = null;

    beforeEach(() => {
        wrapper = shallow(
            <AbcSortableListsContext
                lists={lists}
                onReorder={onReorder}
            >
                {contentRender}
            </AbcSortableListsContext>
        );
    });

    afterEach(() => {
        wrapper.unmount();
        onReorder.mockClear();
    });

    it('Should not call a callback if no destination has been provided', () => {
        wrapper.instance()._onDragEnd({
            source: {
                droppableId: listName,
                index: 0
            },
            destination: null
        });

        expect(onReorder).not.toHaveBeenCalled();
    });

    it('Should not call a callback if the same destination has been provided', () => {
        wrapper.instance()._onDragEnd({
            source: {
                droppableId: listName,
                index: 0
            },
            destination: {
                droppableId: listName,
                index: 0
            }
        });

        expect(onReorder).not.toHaveBeenCalled();
    });
});

describe('Should call `onReorder` and give the same `lists` object', () => {
    const onReorder = jest.fn();

    const firstListName = 'test1';
    const secondListName = 'test2';
    const lists = {
        [firstListName]: {
            items: [
                'one',
                'two',
                'three',
                'four',
                'five'
            ],
            min: 1,
            max: 5,
            isDisabled: true,
            someOtherInfo: 'blahblahblah'
        },
        [secondListName]: {
            items: [
                'six',
                'seven',
                'eight',
                'nine'
            ]
        }
    };

    let wrapper = null;

    beforeEach(() => {
        wrapper = shallow(
            <AbcSortableListsContext
                lists={lists}
                onReorder={onReorder}
            >
                {contentRender}
            </AbcSortableListsContext>
        );
    });

    afterEach(() => {
        wrapper.unmount();
        onReorder.mockClear();
    });

    it('Should move bottom item to the top of the same list and shift other items down', () => {
        const lastIndex = lists[firstListName].items.length - 1;
        const expected = {
            ...lists,
            [firstListName]: {
                ...lists[firstListName],
                items: [
                    'five',
                    'one',
                    'two',
                    'three',
                    'four'
                ]
            }
        };

        wrapper.instance()._onDragEnd({
            source: {
                droppableId: firstListName,
                index: lastIndex
            },
            destination: {
                droppableId: firstListName,
                index: 0
            }
        });

        expect(onReorder).toHaveBeenCalledWith(expected);
    });

    it('Should move top item to the bottom of the same list and shift other items up', () => {
        const lastIndex = lists[firstListName].items.length - 1;
        const expected = {
            ...lists,
            [firstListName]: {
                ...lists[firstListName],
                items: [
                    'two',
                    'three',
                    'four',
                    'five',
                    'one'
                ]
            }
        };

        wrapper.instance()._onDragEnd({
            source: {
                droppableId: firstListName,
                index: 0
            },
            destination: {
                droppableId: firstListName,
                index: lastIndex
            }
        });

        expect(onReorder).toHaveBeenCalledWith(expected);
    });

    it('Should insert middle item of the first list in the middle of the other list', () => {
        const middleIndex = 2;
        const expected = {
            ...lists,
            [firstListName]: {
                ...lists[firstListName],
                items: [
                    'one',
                    'two',
                    'four',
                    'five'
                ]
            },
            [secondListName]: {
                ...lists[secondListName],
                items: [
                    'six',
                    'seven',
                    'three',
                    'eight',
                    'nine'
                ]
            }
        };

        wrapper.instance()._onDragEnd({
            source: {
                droppableId: firstListName,
                index: middleIndex
            },
            destination: {
                droppableId: secondListName,
                index: middleIndex
            }
        });

        expect(onReorder).toHaveBeenCalledWith(expected);
    });
});
