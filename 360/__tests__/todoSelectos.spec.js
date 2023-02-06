import moment from 'moment';
import {OrderedMap, Map, fromJS} from 'immutable';

import TodoListRecord from '../TodoListRecord';
import TodoItemRecord from '../TodoItemRecord';
import {fillStructure} from '../todoReducer';
import {
  getAllLists,
  getCompletedLists,
  getExpiredLists,
  getUncompletedItemsByListId,
  createSelectorForUncompletedItemsByListId,
  createSelectorForCompletedItemsByListId,
  createSelectorForExpiredItemsByListId,
  createSelectorForItemsCountByListId
} from '../todoSelectors';

const fixtures = {
  lists: {
    list1: {
      id: 'list1',
      creationTs: Number(moment('2017-01-10')),
      type: 'todo-list'
    },
    list2: {
      id: 'list2',
      creationTs: Number(moment('2017-01-30')),
      type: 'todo-list'
    },
    list3: {
      id: 'list3',
      creationTs: Number(moment('2017-01-25')),
      type: 'todo-list'
    },
    draft: {
      id: 'draft',
      type: 'todo-list'
    }
  },
  items: {
    list1: {
      common1: {
        uuid: 'list1-common1',
        listId: 'list1',
        position: 20,
        type: 'todo-item'
      },
      common2: {
        uuid: 'list1-common2',
        listId: 'list1',
        position: 19,
        type: 'todo-item'
      },
      completed1: {
        uuid: 'list1-completed1',
        listId: 'list1',
        completed: true,
        position: 5,
        type: 'todo-item'
      },
      completed2: {
        uuid: 'list1-completed2',
        listId: 'list1',
        completed: true,
        position: 7,
        type: 'todo-item'
      },
      expired1: {
        uuid: 'list1-expired1',
        listId: 'list1',
        dueDate: '2017-01-20',
        position: 18,
        type: 'todo-item'
      },
      expired2: {
        uuid: 'list1-expired2',
        listId: 'list1',
        dueDate: '2017-01-10',
        position: 19,
        type: 'todo-item'
      },
      expiredAndCompleted1: {
        uuid: 'list1-expiredAndCompleted1',
        listId: 'list1',
        completed: true,
        dueDate: '2017-01-10',
        position: 6,
        type: 'todo-item'
      }
    },
    list2: {
      expired1: {
        uuid: 'list2-expired1',
        listId: 'list2',
        dueDate: '2017-01-10',
        creationTs: Number(moment('2017-02-05')),
        type: 'todo-item'
      }
    },
    list3: {
      common1: {
        uuid: 'list3-common1',
        listId: 'list3',
        type: 'todo-item'
      },
      completed1: {
        uuid: 'list3-completed1',
        listId: 'list3',
        completed: true,
        type: 'todo-item'
      },
      expiredAndCompleted1: {
        uuid: 'list3-expiredAndCompleted1',
        listId: 'list3',
        completed: true,
        dueDate: '2017-01-10',
        type: 'todo-item'
      }
    }
  }
};

describe('selectors', function() {
  describe('todo', () => {
    describe('getAllLists', () => {
      test('должен вернуть все списки, отсортированные во убыванию даты создания', () => {
        const state = {
          todo: fromJS(
            fillStructure([fixtures.lists.list1, fixtures.lists.list2, fixtures.lists.list3])
          )
        };
        const expected = new OrderedMap([
          [fixtures.lists.list2.id, new TodoListRecord(fixtures.lists.list2)],
          [fixtures.lists.list3.id, new TodoListRecord(fixtures.lists.list3)],
          [fixtures.lists.list1.id, new TodoListRecord(fixtures.lists.list1)]
        ]);

        expect(getAllLists(state)).toEqual(expected);
      });

      test('должен вернуть все списки, отсортированные во убыванию даты создания, черновик всегда в начале', () => {
        const state = {
          todo: fromJS(
            fillStructure([
              fixtures.lists.list1,
              fixtures.lists.list2,
              fixtures.lists.list3,
              fixtures.lists.draft
            ])
          )
        };
        const expected = new OrderedMap([
          [fixtures.lists.draft.id, new TodoListRecord(fixtures.lists.draft)],
          [fixtures.lists.list2.id, new TodoListRecord(fixtures.lists.list2)],
          [fixtures.lists.list3.id, new TodoListRecord(fixtures.lists.list3)],
          [fixtures.lists.list1.id, new TodoListRecord(fixtures.lists.list1)]
        ]);

        expect(getAllLists(state)).toEqual(expected);
      });
    });

    describe('getCompletedLists', () => {
      test('должен вернуть списки, отсортированные во убыванию даты создания, где есть хотя бы одно завершенное дело', () => {
        const state = {
          todo: fromJS(
            fillStructure([
              fixtures.lists.list1,
              fixtures.lists.list2,
              fixtures.lists.list3,
              fixtures.items.list1.completed1,
              fixtures.items.list3.completed1,
              fixtures.items.list3.common1
            ])
          )
        };
        const expected = new OrderedMap([
          [fixtures.lists.list3.id, new TodoListRecord(fixtures.lists.list3)],
          [fixtures.lists.list1.id, new TodoListRecord(fixtures.lists.list1)]
        ]);

        expect(getCompletedLists(state)).toEqual(expected);
      });
    });

    describe('getExpiredLists', () => {
      test('должен вернуть списки, отсортированные во убыванию даты создания, где есть хотя бы одно просроченное дело', () => {
        const state = {
          todo: fromJS(
            fillStructure([
              fixtures.lists.list1,
              fixtures.lists.list2,
              fixtures.lists.list3,
              fixtures.items.list1.expired1,
              fixtures.items.list2.expired1,
              fixtures.items.list3.expiredAndCompleted1
            ])
          ),
          datetime: new Map({
            date: Number(moment('2017-05-12'))
          })
        };
        const expected = new OrderedMap([
          [fixtures.lists.list2.id, new TodoListRecord(fixtures.lists.list2)],
          [fixtures.lists.list1.id, new TodoListRecord(fixtures.lists.list1)]
        ]);

        expect(getExpiredLists(state)).toEqual(expected);
      });
    });

    describe('getUncompletedItemsByListId', () => {
      test('должен вернуть незавершенные дела из указанного списка', () => {
        const state = {
          todo: fromJS(
            fillStructure([
              fixtures.lists.list1,
              fixtures.items.list1.common1,
              fixtures.items.list1.completed1,
              fixtures.items.list1.expired1,
              fixtures.items.list1.common2
            ])
          )
        };
        const expected = new Map({
          [fixtures.items.list1.common1.uuid]: new TodoItemRecord(fixtures.items.list1.common1),
          [fixtures.items.list1.common2.uuid]: new TodoItemRecord(fixtures.items.list1.common2),
          [fixtures.items.list1.expired1.uuid]: new TodoItemRecord(fixtures.items.list1.expired1)
        });
        const listId = fixtures.lists.list1.id;

        expect(getUncompletedItemsByListId(state, listId)).toEqual(expected);
      });

      test('должен вернуть пустой список, если дел нет', () => {
        const state = {
          todo: fromJS(fillStructure([fixtures.lists.list1]))
        };
        const expected = new Map();
        const listId = fixtures.lists.list1.id;

        expect(getUncompletedItemsByListId(state, listId)).toEqual(expected);
      });
    });

    describe('createSelectorForUncompletedItemsByListId', () => {
      test('должен вернуть незавершенные дела, отсортированные по возрастанию позиции', () => {
        const getUncompletedItemsByListId = createSelectorForUncompletedItemsByListId(
          fixtures.lists.list1.id
        );
        const state = {
          todo: fromJS(
            fillStructure([
              fixtures.lists.list1,
              fixtures.items.list1.common1,
              fixtures.items.list1.completed1,
              fixtures.items.list1.expired1,
              fixtures.items.list1.common2
            ])
          )
        };
        const expected = new OrderedMap([
          [fixtures.items.list1.expired1.uuid, new TodoItemRecord(fixtures.items.list1.expired1)],
          [fixtures.items.list1.common2.uuid, new TodoItemRecord(fixtures.items.list1.common2)],
          [fixtures.items.list1.common1.uuid, new TodoItemRecord(fixtures.items.list1.common1)]
        ]);

        expect(getUncompletedItemsByListId(state)).toEqual(expected);
      });

      test('должен вернуть пустой список, если дел нет', () => {
        const getUncompletedItemsByListId = createSelectorForUncompletedItemsByListId(
          fixtures.lists.list1.id
        );
        const state = {
          todo: fromJS(fillStructure([fixtures.lists.list1]))
        };
        const expected = new Map();

        expect(getUncompletedItemsByListId(state)).toEqual(expected);
      });
    });

    describe('createSelectorForCompletedItemsByListId', () => {
      test('должен вернуть завершенные дела, отсортированные по возрастанию позиции', () => {
        const getCompletedItemsByListId = createSelectorForCompletedItemsByListId(
          fixtures.lists.list1.id
        );
        const state = {
          todo: fromJS(
            fillStructure([
              fixtures.lists.list1,
              fixtures.items.list1.completed2,
              fixtures.items.list1.common1,
              fixtures.items.list1.completed1,
              fixtures.items.list1.common2
            ])
          )
        };
        const expected = new OrderedMap([
          [
            fixtures.items.list1.completed1.uuid,
            new TodoItemRecord(fixtures.items.list1.completed1)
          ],
          [
            fixtures.items.list1.completed2.uuid,
            new TodoItemRecord(fixtures.items.list1.completed2)
          ]
        ]);

        expect(getCompletedItemsByListId(state)).toEqual(expected);
      });

      test('должен вернуть пустой список, если дел нет', () => {
        const getCompletedItemsByListId = createSelectorForCompletedItemsByListId(
          fixtures.lists.list1.id
        );
        const state = {
          todo: fromJS(fillStructure([fixtures.lists.list1]))
        };
        const expected = new Map();

        expect(getCompletedItemsByListId(state)).toEqual(expected);
      });
    });

    describe('createSelectorForExpiredItemsByListId', () => {
      test('должен вернуть просроченные дела, отсортированные по возрастанию даты исполнения', () => {
        const getExpiredItemsByListId = createSelectorForExpiredItemsByListId(
          fixtures.lists.list1.id
        );
        const state = {
          todo: fromJS(
            fillStructure([
              fixtures.lists.list1,
              fixtures.items.list1.expired1,
              fixtures.items.list1.completed2,
              fixtures.items.list1.common1,
              fixtures.items.list1.expired2,
              fixtures.items.list1.expiredAndCompleted1
            ])
          ),
          datetime: new Map({
            date: Number(moment('2017-05-12'))
          })
        };
        const expected = new OrderedMap([
          [fixtures.items.list1.expired2.uuid, new TodoItemRecord(fixtures.items.list1.expired2)],
          [fixtures.items.list1.expired1.uuid, new TodoItemRecord(fixtures.items.list1.expired1)]
        ]);

        expect(getExpiredItemsByListId(state)).toEqual(expected);
      });

      test('должен вернуть пустой список, если дел нет', () => {
        const getExpiredItemsByListId = createSelectorForExpiredItemsByListId(
          fixtures.lists.list1.id
        );
        const state = {
          todo: fromJS(fillStructure([fixtures.lists.list1])),
          datetime: new Map({
            date: Number(moment('2017-05-12'))
          })
        };
        const expected = new Map();

        expect(getExpiredItemsByListId(state)).toEqual(expected);
      });
    });

    describe('createSelectorForItemsCountByListId', () => {
      test('должен вернуть количество дел', () => {
        const getItemsCountByListId = createSelectorForItemsCountByListId(fixtures.lists.list1.id);
        const state = {
          todo: fromJS(
            fillStructure([
              fixtures.lists.list1,
              fixtures.items.list1.common1,
              fixtures.items.list1.common2
            ])
          )
        };

        expect(getItemsCountByListId(state)).toBe(2);
      });

      test('должен вернуть 0, если дел нет', () => {
        const getItemsCountByListId = createSelectorForItemsCountByListId(fixtures.lists.list1.id);
        const state = {
          todo: fromJS(fillStructure([fixtures.lists.list1]))
        };

        expect(getItemsCountByListId(state)).toBe(0);
      });
    });
  });
});
