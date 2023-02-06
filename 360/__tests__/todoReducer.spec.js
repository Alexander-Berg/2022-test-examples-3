import {Map, fromJS} from 'immutable';
import moment from 'moment';

import Pathtree from 'utils/Pathtree';
import {ActionTypes as SettingsActionsTypes} from 'features/settings/settingsConstants';

import todo from '../todoReducer';
import {ActionTypes, INITIAL_TODO_STATE as initialState} from '../todoConstants';
import TodoListRecord from '../TodoListRecord';
import TodoItemRecord from '../TodoItemRecord';

const fixtures = {
  lists: {
    draft: {
      id: 'draft'
    },
    '1': {
      id: '1',
      title: 'list-1',
      type: 'todo-list'
    },
    '2': {
      id: '2',
      title: 'list-2',
      type: 'todo-list'
    }
  },
  items: {
    draft: {
      uuid: 'draft-1',
      listId: '1'
    },
    '1': {
      uuid: '1',
      title: 'item-1',
      completed: false,
      position: 10,
      listId: '1',
      type: 'todo-item',
      dueDate: '2012-12-12'
    },
    '2': {
      uuid: '2',
      title: 'item-2',
      completed: false,
      position: 15,
      listId: '1',
      type: 'todo-item'
    },
    '3': {
      uuid: '3',
      title: 'item-3',
      completed: false,
      listId: '2',
      type: 'todo-item'
    },
    '4': {
      uuid: '4',
      title: 'item-4',
      completed: false,
      listId: '2',
      type: 'todo-item'
    },
    '5': {
      uuid: '5',
      title: 'item-5',
      completed: true,
      position: 20,
      listId: '1',
      type: 'todo-item'
    },
    '6': {
      uuid: '6',
      title: 'item-6',
      completed: true,
      position: 30,
      listId: '1',
      type: 'todo-item'
    }
  }
};

function ByUuid(items) {
  return new Map(
    items.reduce((result, id) => {
      if (id instanceof TodoItemRecord) {
        result[id.uuid] = id;
      } else {
        const item = fixtures.items[id];

        result[item.uuid] = new TodoItemRecord(item);
      }
      return result;
    }, {})
  );
}

function ByListId(lists) {
  return new Map(
    lists.reduce((result, item) => {
      const list = fixtures.lists[item.id];
      result[list.id] = new ByUuid(item.items);
      return result;
    }, {})
  );
}

function ByTime(items) {
  const pathtree = new Pathtree();

  return fromJS(
    items.reduce((result, id) => {
      const item = id instanceof TodoItemRecord ? id : new TodoItemRecord(fixtures.items[id]);

      if (item.dueDate) {
        pathtree.traverse(result, item);
      }

      return result;
    }, {})
  );
}

describe('todoReducer', () => {
  test('должен обрабатывать начальное состояние', () => {
    expect(todo(undefined, {})).toEqual(initialState);
  });

  describe('LOAD_TODOS_SUCCESS', () => {
    test('должен добавлять полученные дела в состояние', () => {
      const state = initialState;
      const expectedState = initialState.mergeIn(
        ['items'],
        new Map({
          byUuid: new ByUuid([1, 2, 3, 4]),
          byListId: new ByListId([{id: 1, items: [1, 2]}, {id: 2, items: [3, 4]}]),
          byTime: new ByTime([1, 2, 3, 4])
        })
      );
      const action = {
        type: ActionTypes.LOAD_TODOS_SUCCESS,
        items: [fixtures.items['1'], fixtures.items['2'], fixtures.items['3'], fixtures.items['4']]
      };

      expect(todo(state, action)).toEqual(expectedState);
    });
  });

  describe('LOAD_SIDEBAR_TODOS_START', () => {
    test('Должен выставлять состояние запроса', () => {
      const state = initialState;
      const expectedState = initialState.mergeIn(['sidebar'], {
        isLoading: true,
        isError: false
      });
      const action = {
        type: ActionTypes.LOAD_SIDEBAR_TODOS_START
      };

      expect(todo(state, action)).toEqual(expectedState);
    });
  });

  describe('LOAD_SIDEBAR_TODOS_SUCCESS', () => {
    test('должен добавлять списки и дела в пустой стейт и удалять состояние запроса', () => {
      const state = initialState;
      const expectedState = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1']),
          [fixtures.lists['2'].id]: new TodoListRecord(fixtures.lists['2'])
        }),
        items: new Map({
          byUuid: new ByUuid([1, 2, 3, 4]),
          byListId: new ByListId([{id: 1, items: [1, 2]}, {id: 2, items: [3, 4]}]),
          byTime: new ByTime([1, 2, 3, 4])
        }),
        sidebar: new Map({
          isRequested: true,
          isLoading: false,
          isError: false,
          isLoadingMore: false,
          nextKeys: new Map()
        })
      });
      const action = {
        type: ActionTypes.LOAD_SIDEBAR_TODOS_SUCCESS,
        data: {
          items: [
            fixtures.lists['1'],
            fixtures.lists['2'],
            fixtures.items['1'],
            fixtures.items['2'],
            fixtures.items['3'],
            fixtures.items['4']
          ],
          nextKeys: {}
        }
      };

      expect(todo(state, action)).toEqual(expectedState);
    });

    test('должен добавлять списки и дела в не пустой стейт и удалять состояние запроса', () => {
      const state = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: new Map({
          byUuid: new ByUuid([1, 2]),
          byListId: new ByListId([{id: 1, items: [1, 2]}]),
          byTime: new ByTime([1, 2])
        }),
        sidebar: new Map({
          isRequested: false,
          isLoading: false,
          isError: false,
          isLoadingMore: false,
          nextKeys: new Map()
        })
      });
      const expectedState = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1']),
          [fixtures.lists['2'].id]: new TodoListRecord(fixtures.lists['2'])
        }),
        items: new Map({
          byUuid: new ByUuid([1, 2, 3, 4]),
          byListId: new ByListId([{id: 1, items: [1, 2]}, {id: 2, items: [3, 4]}]),
          byTime: new ByTime([1, 2, 3, 4])
        }),
        sidebar: new Map({
          isRequested: true,
          isLoading: false,
          isError: false,
          isLoadingMore: false,
          nextKeys: new Map()
        })
      });
      const action = {
        type: ActionTypes.LOAD_SIDEBAR_TODOS_SUCCESS,
        data: {
          items: [fixtures.lists['2'], fixtures.items['3'], fixtures.items['4']],
          nextKeys: {}
        }
      };

      expect(todo(state, action)).toEqual(expectedState);
    });
  });

  describe('LOAD_SIDEBAR_TODOS_FAILURE', () => {
    test('Должен выставлять состояние ошибки и удалять состояние запроса', () => {
      const state = initialState.merge({
        sidebar: {
          isLoading: true,
          isError: false,
          isLoadingMore: false,
          nextKeys: {}
        }
      });
      const expectedState = initialState.merge({
        sidebar: {
          isLoading: false,
          isError: true,
          isLoadingMore: false,
          nextKeys: {}
        }
      });
      const action = {
        type: ActionTypes.LOAD_SIDEBAR_TODOS_FAILURE
      };

      expect(todo(state, action)).toEqual(expectedState);
    });
  });

  describe('LOAD_MORE_SIDEBAR_TODOS', () => {
    test('Должен выставлять состояние подгрузки дел', () => {
      const state = initialState;
      const expectedState = initialState.mergeIn(['sidebar'], {
        isLoadingMore: true
      });
      const action = {
        type: ActionTypes.LOAD_MORE_SIDEBAR_TODOS
      };

      expect(todo(state, action)).toEqual(expectedState);
    });
  });

  describe('LOAD_MORE_SIDEBAR_TODOS_SUCCESS', () => {
    test('Должен добавлять новые данные и удалять состояние подгрузки дел', () => {
      const state = initialState.merge({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: new Map({
          byUuid: new ByUuid([1, 2]),
          byListId: new ByListId([{id: 1, items: [1, 2]}]),
          byTime: new ByTime([1, 2])
        }),
        sidebar: {
          isLoading: false,
          isError: false,
          isLoadingMore: true,
          nextKeys: {
            planned: 'planned-key-1',
            expired: 'expired-key-1'
          }
        }
      });
      const expectedState = initialState.merge({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1']),
          [fixtures.lists['2'].id]: new TodoListRecord(fixtures.lists['2'])
        }),
        items: new Map({
          byUuid: new ByUuid([1, 2, 3, 4]),
          byListId: new ByListId([{id: 1, items: [1, 2]}, {id: 2, items: [3, 4]}]),
          byTime: new ByTime([1, 2, 3, 4])
        }),
        sidebar: {
          isLoading: false,
          isError: false,
          isLoadingMore: false,
          nextKeys: {
            planned: 'planned-key-2',
            expired: 'expired-key-1'
          }
        }
      });
      const action = {
        type: ActionTypes.LOAD_MORE_SIDEBAR_TODOS_SUCCESS,
        data: {
          items: [fixtures.lists['2'], fixtures.items['3'], fixtures.items['4']],
          nextKeys: {
            planned: 'planned-key-2'
          }
        },
        todosType: 'planned'
      };

      expect(todo(state, action)).toEqual(expectedState);
    });
  });

  describe('LOAD_MORE_SIDEBAR_TODOS_FAILURE', () => {
    test('Должен удалять состояние подгрузки дел', () => {
      const state = initialState.merge({
        sidebar: {
          isLoading: false,
          isError: false,
          isLoadingMore: true,
          nextKeys: {}
        }
      });
      const expectedState = initialState.merge({
        sidebar: {
          isLoading: false,
          isError: false,
          isLoadingMore: false,
          nextKeys: {}
        }
      });
      const action = {
        type: ActionTypes.LOAD_MORE_SIDEBAR_TODOS_FAILURE
      };

      expect(todo(state, action)).toEqual(expectedState);
    });
  });

  describe('CREATE_DRAFT_LIST', () => {
    test('должен добавлять черновик нового списка', () => {
      const state = initialState;
      const expectedState = new Map({
        lists: new Map({
          [fixtures.lists.draft.id]: new TodoListRecord(fixtures.lists.draft)
        }),
        items: initialState.get('items'),
        sidebar: initialState.get('sidebar')
      });
      const action = {
        type: ActionTypes.CREATE_DRAFT_LIST
      };

      expect(todo(state, action)).toEqual(expectedState);
    });
  });

  describe('DELETE_DRAFT_LIST', () => {
    test('должен удалять черновик нового списка', () => {
      const state = new Map({
        lists: new Map({
          [fixtures.lists.draft.id]: new TodoListRecord(fixtures.lists.draft),
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: initialState.get('items'),
        sidebar: initialState.get('sidebar')
      });
      const expectedState = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: initialState.get('items'),
        sidebar: initialState.get('sidebar')
      });
      const action = {
        type: ActionTypes.DELETE_DRAFT_LIST
      };

      expect(todo(state, action)).toEqual(expectedState);
    });
  });

  describe('CREATE_LIST_SUCCESS', () => {
    test('должен добавлять новый список и удалять черновик', () => {
      const state = new Map({
        lists: new Map({
          [fixtures.lists.draft.id]: new TodoListRecord(fixtures.lists.draft)
        }),
        items: initialState.get('items'),
        sidebar: initialState.get('sidebar')
      });
      const expectedState = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: initialState.get('items'),
        sidebar: initialState.get('sidebar')
      });
      const action = {
        type: ActionTypes.CREATE_LIST_SUCCESS,
        list: fixtures.lists['1']
      };

      expect(todo(state, action)).toEqual(expectedState);
    });
  });

  describe('UPDATE_LIST_SUCCESS', () => {
    test('должен обновлять список', () => {
      const newList = new TodoListRecord(
        Object.assign({}, fixtures.lists['1'], {
          title: 'new list'
        })
      );

      const state = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1']),
          [fixtures.lists['2'].id]: new TodoListRecord(fixtures.lists['2'])
        }),
        items: initialState.get('items'),
        sidebar: initialState.get('sidebar')
      });
      const expectedState = new Map({
        lists: new Map({
          [newList.id]: newList,
          [fixtures.lists['2'].id]: new TodoListRecord(fixtures.lists['2'])
        }),
        items: initialState.get('items'),
        sidebar: initialState.get('sidebar')
      });
      const action = {
        type: ActionTypes.UPDATE_LIST_SUCCESS,
        id: fixtures.lists['1'].id,
        params: {
          title: newList.title
        }
      };

      expect(todo(state, action)).toEqual(expectedState);
    });
  });

  describe('DELETE_LIST_SUCCESS', () => {
    test('должен удалять список', () => {
      const state = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1']),
          [fixtures.lists['2'].id]: new TodoListRecord(fixtures.lists['2'])
        }),
        items: new Map({
          byUuid: new ByUuid([1, 3]),
          byListId: new ByListId([{id: 1, items: [1]}, {id: 2, items: [3]}]),
          byTime: new ByTime([1, 3])
        }),
        sidebar: initialState.get('sidebar')
      });
      const expectedState = new Map({
        lists: new Map({
          [fixtures.lists['2'].id]: new TodoListRecord(fixtures.lists['2'])
        }),
        items: new Map({
          byUuid: new ByUuid([3]),
          byListId: new ByListId([{id: 2, items: [3]}]),
          byTime: new ByTime([3])
        }),
        sidebar: initialState.get('sidebar')
      });
      const action = {
        type: ActionTypes.DELETE_LIST_SUCCESS,
        id: fixtures.lists['1'].id
      };

      expect(todo(state, action)).toEqual(expectedState);
    });
  });

  describe('CREATE_DRAFT_ITEM', () => {
    test('должен добавлять черновик нового дела в конец списка незавершенных дел', () => {
      const draft = new TodoItemRecord(
        Object.assign({}, fixtures.items.draft, {
          position: fixtures.items['2'].position + 1
        })
      );

      const state = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: new Map({
          byUuid: new ByUuid([1, 2, 5]),
          byListId: new ByListId([{id: 1, items: [1, 2, 5]}]),
          byTime: new ByTime([1, 2, 5])
        }),
        sidebar: initialState.get('sidebar')
      });
      const expectedState = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: new Map({
          byUuid: new ByUuid([1, 2, 5, draft]),
          byListId: new ByListId([{id: 1, items: [1, 2, 5, draft]}]),
          byTime: new ByTime([1, 2, 5])
        }),
        sidebar: initialState.get('sidebar')
      });
      const action = {
        type: ActionTypes.CREATE_DRAFT_ITEM,
        listId: fixtures.lists['1'].id
      };

      expect(todo(state, action)).toEqual(expectedState);
    });
  });

  describe('DELETE_DRAFT_ITEM', () => {
    test('должен удалять черновик нового дела', () => {
      const state = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: new Map({
          byUuid: new ByUuid(['draft']),
          byListId: new ByListId([{id: 1, items: ['draft']}]),
          byTime: new ByTime(['draft'])
        }),
        sidebar: initialState.get('sidebar')
      });
      const expectedState = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: new Map({
          byUuid: new ByUuid([]),
          byListId: new ByListId([]),
          byTime: new ByTime([])
        }),
        sidebar: initialState.get('sidebar')
      });
      const action = {
        type: ActionTypes.DELETE_DRAFT_ITEM,
        listId: fixtures.lists['1'].id
      };

      expect(todo(state, action)).toEqual(expectedState);
    });
  });

  describe('CREATE_ITEM_SUCCESS', () => {
    test('должен добавлять новое дело на место черновика и удалять черновик', () => {
      const draft = new TodoItemRecord(
        Object.assign({}, fixtures.items.draft, {
          position: fixtures.items['2'].position + 1
        })
      );
      const newItem = Object.assign({}, fixtures.items['1'], {
        position: draft.position
      });

      const state = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: new Map({
          byUuid: new ByUuid([2, 5, draft]),
          byListId: new ByListId([{id: 1, items: [2, 5, draft]}]),
          byTime: new ByTime([2, 5, draft])
        }),
        sidebar: initialState.get('sidebar')
      });

      const item = new TodoItemRecord(newItem);
      const expectedState = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: new Map({
          byUuid: new ByUuid([2, 5, item]),
          byListId: new ByListId([{id: 1, items: [2, 5, item]}]),
          byTime: new ByTime([2, 5, item])
        }),
        sidebar: initialState.get('sidebar')
      });
      const action = {
        type: ActionTypes.CREATE_ITEM_SUCCESS,
        item: newItem
      };

      expect(todo(state, action)).toEqual(expectedState);
    });
  });

  describe('UPDATE_ITEM_SUCCESS', () => {
    test('должен обновлять дело', () => {
      const newItem = new TodoItemRecord(
        Object.assign({}, fixtures.items['1'], {
          title: 'new item'
        })
      );

      const state = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: new Map({
          byUuid: new ByUuid([1]),
          byListId: new ByListId([{id: 1, items: [1]}]),
          byTime: new ByTime([1])
        }),
        sidebar: initialState.get('sidebar')
      });
      const expectedState = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: new Map({
          byUuid: new ByUuid([newItem]),
          byListId: new ByListId([{id: 1, items: [newItem]}]),
          byTime: new ByTime([newItem])
        }),
        sidebar: initialState.get('sidebar')
      });
      const action = {
        type: ActionTypes.UPDATE_ITEM_SUCCESS,
        itemId: fixtures.items['1'].uuid,
        listId: fixtures.items['1'].listId,
        params: {
          title: newItem.title
        }
      };

      expect(todo(state, action)).toEqual(expectedState);
    });

    describe('время исполнения', () => {
      test('должен корректно обновлять время исполнения', () => {
        const newItem = new TodoItemRecord(
          Object.assign({}, fixtures.items['1'], {
            dueDate: '2012-12-20'
          })
        );

        const state = new Map({
          lists: new Map({
            [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
          }),
          items: new Map({
            byUuid: new ByUuid([1]),
            byListId: new ByListId([{id: 1, items: [1]}]),
            byTime: new ByTime([1])
          }),
          sidebar: initialState.get('sidebar')
        });
        const expectedState = new Map({
          lists: new Map({
            [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
          }),
          items: new Map({
            byUuid: new ByUuid([newItem]),
            byListId: new ByListId([{id: 1, items: [newItem]}]),
            byTime: new ByTime([newItem])
          }),
          sidebar: initialState.get('sidebar')
        });
        const action = {
          type: ActionTypes.UPDATE_ITEM_SUCCESS,
          itemId: fixtures.items['1'].uuid,
          listId: fixtures.items['1'].listId,
          params: {
            dueDate: newItem.dueDate
          }
        };

        expect(todo(state, action)).toEqual(expectedState);
      });

      test('должен корректно удалять время исполнения', () => {
        const newItem = new TodoItemRecord(
          Object.assign({}, fixtures.items['1'], {
            dueDate: null
          })
        );

        const state = new Map({
          lists: new Map({
            [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
          }),
          items: new Map({
            byUuid: new ByUuid([1]),
            byListId: new ByListId([{id: 1, items: [1]}]),
            byTime: new ByTime([1])
          }),
          sidebar: initialState.get('sidebar')
        });
        const expectedState = new Map({
          lists: new Map({
            [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
          }),
          items: new Map({
            byUuid: new ByUuid([newItem]),
            byListId: new ByListId([{id: 1, items: [newItem]}]),
            byTime: new ByTime([newItem])
          }),
          sidebar: initialState.get('sidebar')
        });
        const action = {
          type: ActionTypes.UPDATE_ITEM_SUCCESS,
          itemId: fixtures.items['1'].uuid,
          listId: fixtures.items['1'].listId,
          params: {
            dueDate: newItem.dueDate
          }
        };

        expect(todo(state, action)).toEqual(expectedState);
      });
    });

    describe('чек/анчек', () => {
      test('должен переместить дело в конец списка завершенных, если сделали его завершенным', () => {
        const newItem = new TodoItemRecord(
          Object.assign({}, fixtures.items['1'], {
            completed: true,
            position: fixtures.items['6'].position + 1
          })
        );

        const state = new Map({
          lists: new Map({
            [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
          }),
          items: new Map({
            byUuid: new ByUuid([1, 5, 6]),
            byListId: new ByListId([{id: 1, items: [1, 5, 6]}]),
            byTime: new ByTime([1, 5, 6])
          }),
          sidebar: initialState.get('sidebar')
        });
        const expectedState = new Map({
          lists: new Map({
            [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
          }),
          items: new Map({
            byUuid: new ByUuid([newItem, 5, 6]),
            byListId: new ByListId([{id: 1, items: [newItem, 5, 6]}]),
            byTime: new ByTime([newItem, 5, 6])
          }),
          sidebar: initialState.get('sidebar')
        });
        const action = {
          type: ActionTypes.UPDATE_ITEM_SUCCESS,
          itemId: fixtures.items['1'].uuid,
          listId: fixtures.items['1'].listId,
          params: {
            completed: true
          }
        };

        expect(todo(state, action)).toEqual(expectedState);
      });

      test('должен переместить дело в конец списка незавершенных, если сделали его незавершенным', () => {
        const newItem = new TodoItemRecord(
          Object.assign({}, fixtures.items['5'], {
            completed: false,
            position: fixtures.items['2'].position + 1
          })
        );

        const state = new Map({
          lists: new Map({
            [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
          }),
          items: new Map({
            byUuid: new ByUuid([1, 2, 5]),
            byListId: new ByListId([{id: 1, items: [1, 2, 5]}]),
            byTime: new ByTime([1, 2, 5])
          }),
          sidebar: initialState.get('sidebar')
        });
        const expectedState = new Map({
          lists: new Map({
            [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
          }),
          items: new Map({
            byUuid: new ByUuid([newItem, 1, 2]),
            byListId: new ByListId([{id: 1, items: [newItem, 1, 2]}]),
            byTime: new ByTime([newItem, 1, 2])
          }),
          sidebar: initialState.get('sidebar')
        });
        const action = {
          type: ActionTypes.UPDATE_ITEM_SUCCESS,
          itemId: fixtures.items['5'].uuid,
          listId: fixtures.items['5'].listId,
          params: {
            completed: false
          }
        };

        expect(todo(state, action)).toEqual(expectedState);
      });

      test('должен учитывать переданную позицию при чеке/анчеке, а не вычислять новую', () => {
        const newItem = new TodoItemRecord(
          Object.assign({}, fixtures.items['5'], {
            completed: false,
            position: 100
          })
        );

        const state = new Map({
          lists: new Map({
            [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
          }),
          items: new Map({
            byUuid: new ByUuid([1, 2, 5]),
            byListId: new ByListId([{id: 1, items: [1, 2, 5]}]),
            byTime: new ByTime([1, 2, 5])
          }),
          sidebar: initialState.get('sidebar')
        });
        const expectedState = new Map({
          lists: new Map({
            [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
          }),
          items: new Map({
            byUuid: new ByUuid([newItem, 1, 2]),
            byListId: new ByListId([{id: 1, items: [newItem, 1, 2]}]),
            byTime: new ByTime([newItem, 1, 2])
          }),
          sidebar: initialState.get('sidebar')
        });
        const action = {
          type: ActionTypes.UPDATE_ITEM_SUCCESS,
          itemId: fixtures.items['5'].uuid,
          listId: fixtures.items['5'].listId,
          params: {
            completed: false,
            position: 100
          }
        };

        expect(todo(state, action)).toEqual(expectedState);
      });
    });
  });

  describe('DELETE_ITEM_SUCCESS', () => {
    test('должен удалять дело из списка', () => {
      const state = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: new Map({
          byUuid: new ByUuid([1, 2]),
          byListId: new ByListId([{id: 1, items: [1, 2]}]),
          byTime: new ByTime([1, 2])
        }),
        sidebar: initialState.get('sidebar')
      });
      const expectedState = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: new Map({
          byUuid: new ByUuid([1]),
          byListId: new ByListId([{id: 1, items: [1]}]),
          byTime: new ByTime([1])
        }),
        sidebar: initialState.get('sidebar')
      });
      const action = {
        type: ActionTypes.DELETE_ITEM_SUCCESS,
        itemId: fixtures.items['2'].uuid,
        listId: fixtures.items['2'].listId
      };

      expect(todo(state, action)).toEqual(expectedState);
    });
  });

  describe('SWAP_ITEMS', () => {
    test('должен менять дела местами', () => {
      const before = [1, 2, 5, 6];
      const after = [
        new TodoItemRecord({...fixtures.items['2'], position: 0}),
        new TodoItemRecord({...fixtures.items['5'], position: 1}),
        new TodoItemRecord({...fixtures.items['6'], position: 2}),
        new TodoItemRecord({...fixtures.items['1'], position: 3})
      ];

      const state = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: new Map({
          byUuid: new ByUuid(before),
          byListId: new ByListId([{id: 1, items: before}]),
          byTime: new ByTime(before)
        }),
        sidebar: initialState.get('sidebar')
      });
      const expectedState = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: new Map({
          byUuid: new ByUuid(after),
          byListId: new ByListId([{id: 1, items: after}]),
          byTime: new ByTime(after)
        }),
        sidebar: initialState.get('sidebar')
      });
      const action = {
        type: ActionTypes.SWAP_ITEMS,
        itemId1: fixtures.items['1'].uuid,
        itemId2: fixtures.items['6'].uuid,
        listId: fixtures.items['1'].listId
      };

      expect(todo(state, action)).toEqual(expectedState);
    });
  });

  describe('Settings/UPDATE_SETTINGS_SUCCESS', () => {
    test('не должен реагировать, если не меняли начало недели', () => {
      const state = initialState;
      const expectedState = initialState;
      const action = {
        type: SettingsActionsTypes.UPDATE_SETTINGS_SUCCESS,
        oldSettings: {
          defaultView: 'week'
        },
        newSettings: {
          defaultView: 'month'
        }
      };

      expect(todo(state, action)).toEqual(expectedState);
    });

    test('не должен реагировать, если новое начало недели равняется предыдущему', () => {
      const state = initialState;
      const expectedState = initialState;
      const action = {
        type: SettingsActionsTypes.UPDATE_SETTINGS_SUCCESS,
        oldSettings: {
          weekStartDay: 1
        },
        newSettings: {
          weekStartDay: 1
        }
      };

      expect(todo(state, action)).toEqual(expectedState);
    });

    test('должен обновить weekTimestamp у всех дел с dueDate', () => {
      const oldWeekStartDay = moment.localeData().firstDayOfWeek();
      const newWeekStartDay = (oldWeekStartDay + 1) % 6;

      const state = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: new Map({
          byUuid: new ByUuid([1, 2]),
          byListId: new ByListId([{id: 1, items: [1, 2]}]),
          byTime: new ByTime([1, 2])
        }),
        sidebar: initialState.get('sidebar')
      });

      moment.updateLocale(moment.locale(), {
        week: {
          dow: newWeekStartDay
        }
      });

      const expectedState = new Map({
        lists: new Map({
          [fixtures.lists['1'].id]: new TodoListRecord(fixtures.lists['1'])
        }),
        items: new Map({
          byUuid: new ByUuid([1, 2]),
          byListId: new ByListId([{id: 1, items: [1, 2]}]),
          byTime: new ByTime([1, 2])
        }),
        sidebar: initialState.get('sidebar')
      });

      const action = {
        type: SettingsActionsTypes.UPDATE_SETTINGS_SUCCESS,
        oldSettings: {
          weekStartDay: oldWeekStartDay
        },
        newSettings: {
          weekStartDay: newWeekStartDay
        }
      };

      expect(todo(state, action)).toEqual(expectedState);

      moment.updateLocale(moment.locale(), {
        week: {
          dow: oldWeekStartDay
        }
      });
    });
  });
});
