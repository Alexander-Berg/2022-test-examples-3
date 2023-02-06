import React from 'react';
import { shallow } from 'enzyme';

import AbcSortableListsContext from 'b:abc-sortable-lists-context m:overflowable';

const contentRender = () => 'List of lists goes here...';

describe('AbcSortableListsContext', () => {
    it('Should render context of overflowable sortable lists', () => {
        const wrapper = shallow(
            <AbcSortableListsContext
                overflowable
                lists={{}}
                order={[]}
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

describe('Should call `onReorder` from overflowable lists', () => {
    const onReorder = jest.fn();

    const order = [
        'test1',
        'test2',
        'test3'
    ];
    const lists = {
        [order[0]]: {
            items: [
                'one',
                'two',
                'three'
            ],
            min: 3,
            max: 3
        },
        [order[1]]: {
            items: [
                'four',
                'five',
                'six'
            ],
            min: 3,
            max: 3
        },
        [order[2]]: {
            items: [
                'seven',
                'eight',
                'nine'
            ],
            min: 3,
            max: 3
        }
    };

    let wrapper = null;

    beforeEach(() => {
        wrapper = shallow(
            <AbcSortableListsContext
                overflowable
                lists={lists}
                order={order}
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
        const changingListName = order[1];

        const expected = {
            ...lists,
            [changingListName]: {
                ...lists[changingListName],
                items: [
                    'six',
                    'four',
                    'five'
                ]
            }
        };

        wrapper.instance()._onDragEnd({
            source: {
                droppableId: changingListName,
                index: 2
            },
            destination: {
                droppableId: changingListName,
                index: 0
            }
        });

        expect(onReorder).toHaveBeenCalledWith(expected);
    });

    it('Should move bottom element of the last list to the top of the first one and perform downward overflow', () => {
        const firstListName = order[0];
        const middleListName = order[1];
        const lastListName = order[order.length - 1];
        const expected = {
            [firstListName]: {
                ...lists[firstListName],
                items: [
                    'nine',
                    'one',
                    'two'
                ]
            },
            [middleListName]: {
                ...lists[middleListName],
                items: [
                    'three',
                    'four',
                    'five'
                ]
            },
            [lastListName]: {
                ...lists[lastListName],
                items: [
                    'six',
                    'seven',
                    'eight'
                ]
            }
        };

        wrapper.instance()._onDragEnd({
            source: {
                droppableId: lastListName,
                index: lists[lastListName].items.length - 1
            },
            destination: {
                droppableId: firstListName,
                index: 0
            }
        });

        expect(onReorder).toHaveBeenCalledWith(expected);
    });

    it('Should move top element of the first list to the bottom of the last one and perform upward overflow', () => {
        const firstListName = order[0];
        const middleListName = order[1];
        const lastListName = order[order.length - 1];
        const expected = {
            [firstListName]: {
                ...lists[firstListName],
                items: [
                    'two',
                    'three',
                    'four'
                ]
            },
            [middleListName]: {
                ...lists[middleListName],
                items: [
                    'five',
                    'six',
                    'seven'
                ]
            },
            [lastListName]: {
                ...lists[lastListName],
                items: [
                    'eight',
                    'nine',
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
                droppableId: lastListName,
                index: lists[lastListName].items.length
            }
        });

        expect(onReorder).toHaveBeenCalledWith(expected);
    });
});
