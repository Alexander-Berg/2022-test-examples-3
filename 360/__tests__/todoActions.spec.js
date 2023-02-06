import createActionMetaInfo from 'middlewares/offlineMiddleware/utils/createActionMetaInfo';

import {ActionTypes} from '../todoConstants';
import {
  loadTodos,
  loadTodosNetwork,
  loadTodosDone,
  loadTodosSuccess,
  loadSidebarTodos,
  loadSidebarTodosNetwork,
  loadSidebarTodosOffline,
  loadSidebarTodosStart,
  loadSidebarTodosSuccess,
  loadSidebarTodosFailure,
  loadMoreSidebarTodos,
  loadMoreSidebarTodosNetwork,
  loadMoreSidebarTodosOffline,
  loadMoreSidebarTodosSuccess,
  loadMoreSidebarTodosFailure,
  createDraftList,
  createList,
  createListSuccess,
  editList,
  editListSuccess,
  editListFailure,
  deleteDraftList,
  deleteList,
  deleteListSuccess,
  createDraftItem,
  createItem,
  createItemSuccess,
  editItem,
  editItemSuccess,
  editItemFailure,
  deleteDraftItem,
  deleteItem,
  deleteItemSuccess,
  swapItems,
  reorderItems,
  postpointDraftItemCreation
} from '../todoActions';

describe('todoActions', () => {
  describe('loadTodos', () => {
    test('должен вернуть экшен LOAD_TODOS', () => {
      const dueFrom = 1;
      const dueTo = 2;

      expect(loadTodos({dueFrom, dueTo})).toEqual({
        type: ActionTypes.LOAD_TODOS,
        payload: {
          dueFrom,
          dueTo
        },
        meta: createActionMetaInfo({
          network: loadTodosNetwork({dueFrom, dueTo}),
          rollback: loadTodosDone({dueFrom, dueTo})
        })
      });
    });
  });

  describe('loadTodosDone', () => {
    test('должен вернуть экшен LOAD_TODOS_DONE', () => {
      const payload = {
        dueFrom: 1,
        dueTo: 2
      };

      expect(loadTodosDone(payload)).toEqual({
        type: ActionTypes.LOAD_TODOS_DONE,
        payload
      });
    });
  });

  describe('loadTodosSuccess', () => {
    test('должен вернуть экшен LOAD_TODOS_SUCCESS', () => {
      const items = [];

      expect(loadTodosSuccess(items)).toEqual({
        type: ActionTypes.LOAD_TODOS_SUCCESS,
        items
      });
    });
  });

  describe('loadSidebarTodos', () => {
    test('должен вернуть экшен LOAD_SIDEBAR_TODOS', () => {
      const payload = {};
      const resolve = () => {};

      expect(loadSidebarTodos(payload, resolve)).toEqual({
        type: ActionTypes.LOAD_SIDEBAR_TODOS,
        payload: {
          ...payload,
          resolve
        },
        meta: createActionMetaInfo({
          network: loadSidebarTodosNetwork({...payload, resolve}),
          rollback: loadSidebarTodosOffline({...payload, resolve})
        })
      });
    });
  });

  describe('loadSidebarTodosStart', () => {
    test('должен вернуть экшен LOAD_SIDEBAR_TODOS_START', () => {
      expect(loadSidebarTodosStart()).toEqual({
        type: ActionTypes.LOAD_SIDEBAR_TODOS_START
      });
    });
  });

  describe('loadSidebarTodosSuccess', () => {
    test('должен вернуть экшен LOAD_SIDEBAR_TODOS_SUCCESS', () => {
      expect(loadSidebarTodosSuccess([])).toEqual({
        type: ActionTypes.LOAD_SIDEBAR_TODOS_SUCCESS,
        data: []
      });
    });
  });

  describe('loadSidebarTodosFailure', () => {
    test('должен вернуть экшен LOAD_SIDEBAR_TODOS_FAILURE', () => {
      expect(loadSidebarTodosFailure()).toEqual({
        type: ActionTypes.LOAD_SIDEBAR_TODOS_FAILURE
      });
    });
  });

  describe('loadMoreSidebarTodos', () => {
    test('должен вернуть экшен LOAD_MORE_SIDEBAR_TODOS', () => {
      const params = {
        todosType: 'planned',
        nextKey: 'some-key',
        resolve() {}
      };
      expect(loadMoreSidebarTodos(params)).toEqual({
        type: ActionTypes.LOAD_MORE_SIDEBAR_TODOS,
        payload: {
          todosType: params.todosType,
          nextKey: params.nextKey,
          resolve: params.resolve
        },
        meta: createActionMetaInfo({
          network: loadMoreSidebarTodosNetwork(params),
          rollback: loadMoreSidebarTodosOffline(params)
        })
      });
    });
  });

  describe('loadMoreSidebarTodosSuccess', () => {
    test('должен вернуть экшен LOAD_MORE_SIDEBAR_TODOS_SUCCESS', () => {
      const params = {
        data: {items: []},
        todosType: 'planned'
      };
      expect(loadMoreSidebarTodosSuccess(params)).toEqual({
        type: ActionTypes.LOAD_MORE_SIDEBAR_TODOS_SUCCESS,
        data: params.data,
        todosType: params.todosType
      });
    });
  });

  describe('loadMoreSidebarTodosFailure', () => {
    test('должен вернуть экшен LOAD_MORE_SIDEBAR_TODOS_FAILURE', () => {
      expect(loadMoreSidebarTodosFailure()).toEqual({
        type: ActionTypes.LOAD_MORE_SIDEBAR_TODOS_FAILURE
      });
    });
  });

  describe('createDraftList', () => {
    test('должен вернуть экшен CREATE_DRAFT_LIST', () => {
      expect(createDraftList()).toEqual({
        type: ActionTypes.CREATE_DRAFT_LIST
      });
    });
  });

  describe('createList', () => {
    test('должен вернуть экшен CREATE_LIST', () => {
      const params = {
        title: 'title'
      };
      expect(createList(params)).toEqual({
        type: ActionTypes.CREATE_LIST,
        params
      });
    });
  });

  describe('createListSuccess', () => {
    test('должен вернуть экшен CREATE_LIST_SUCCESS', () => {
      const list = {
        title: 'title'
      };
      expect(createListSuccess(list)).toEqual({
        type: ActionTypes.CREATE_LIST_SUCCESS,
        list
      });
    });
  });

  describe('editList', () => {
    test('должен вернуть экшен UPDATE_LIST', () => {
      const params = {
        id: '1',
        newParams: {
          title: 'new title'
        },
        oldParams: {
          title: 'old title'
        }
      };
      expect(editList(params)).toEqual({
        type: ActionTypes.UPDATE_LIST,
        id: params.id,
        newParams: params.newParams,
        oldParams: params.oldParams
      });
    });
  });

  describe('editListSuccess', () => {
    test('должен вернуть экшен UPDATE_LIST_SUCCESS', () => {
      const params = {
        id: '1',
        params: {
          title: 'new title'
        }
      };
      expect(editListSuccess(params)).toEqual({
        type: ActionTypes.UPDATE_LIST_SUCCESS,
        id: params.id,
        params: params.params
      });
    });
  });

  describe('editListFailure', () => {
    test('должен вернуть экшен UPDATE_LIST_FAILURE', () => {
      const params = {
        id: '1',
        params: {
          title: 'old title'
        }
      };
      expect(editListFailure(params)).toEqual({
        type: ActionTypes.UPDATE_LIST_FAILURE,
        id: params.id,
        params: params.params
      });
    });
  });

  describe('deleteDraftList', () => {
    test('должен вернуть экшен DELETE_DRAFT_LIST', () => {
      expect(deleteDraftList()).toEqual({
        type: ActionTypes.DELETE_DRAFT_LIST
      });
    });
  });

  describe('deleteList', () => {
    test('должен вернуть экшен DELETE_LIST', () => {
      expect(deleteList('1')).toEqual({
        type: ActionTypes.DELETE_LIST,
        id: '1'
      });
    });
  });

  describe('deleteListSuccess', () => {
    test('должен вернуть экшен DELETE_LIST_SUCCESS', () => {
      expect(deleteListSuccess('1')).toEqual({
        type: ActionTypes.DELETE_LIST_SUCCESS,
        id: '1'
      });
    });
  });

  describe('createDraftItem', () => {
    test('должен вернуть экшен CREATE_DRAFT_ITEM', () => {
      expect(createDraftItem('1')).toEqual({
        type: ActionTypes.CREATE_DRAFT_ITEM,
        listId: '1'
      });
    });
  });

  describe('createItem', () => {
    test('должен вернуть экшен CREATE_ITEM', () => {
      const params = {
        listId: '1',
        enter: true,
        params: {title: 'new item'}
      };
      expect(createItem(params)).toEqual({
        type: ActionTypes.CREATE_ITEM,
        listId: params.listId,
        params: params.params,
        enter: params.enter
      });
    });
  });

  describe('createItemSuccess', () => {
    test('должен вернуть экшен CREATE_ITEM_SUCCESS', () => {
      const params = {
        id: '1',
        listId: '1',
        title: 'new item'
      };
      expect(createItemSuccess(params)).toEqual({
        type: ActionTypes.CREATE_ITEM_SUCCESS,
        item: params
      });
    });
  });

  describe('editItem', () => {
    test('должен вернуть экшен UPDATE_ITEM', () => {
      const params = {
        itemId: '1',
        listId: '1',
        newParams: {
          title: 'new title'
        },
        oldParams: {
          title: 'old title'
        }
      };
      expect(editItem(params)).toEqual({
        type: ActionTypes.UPDATE_ITEM,
        itemId: params.itemId,
        listId: params.listId,
        newParams: params.newParams,
        oldParams: params.oldParams
      });
    });
  });

  describe('editItemSuccess', () => {
    test('должен вернуть экшен UPDATE_ITEM_SUCCESS', () => {
      const params = {
        itemId: '1',
        listId: '1',
        params: {
          title: 'new title'
        }
      };
      expect(editItemSuccess(params)).toEqual({
        type: ActionTypes.UPDATE_ITEM_SUCCESS,
        itemId: params.itemId,
        listId: params.listId,
        params: params.params
      });
    });
  });

  describe('editItemFailure', () => {
    test('должен вернуть экшен UPDATE_ITEM_FAILURE', () => {
      const params = {
        itemId: '1',
        listId: '1',
        params: {
          title: 'old title'
        }
      };
      expect(editItemFailure(params)).toEqual({
        type: ActionTypes.UPDATE_ITEM_FAILURE,
        itemId: params.itemId,
        listId: params.listId,
        params: params.params
      });
    });
  });

  describe('deleteDraftItem', () => {
    test('должен вернуть экшен DELETE_DRAFT_ITEM', () => {
      expect(deleteDraftItem('1')).toEqual({
        type: ActionTypes.DELETE_DRAFT_ITEM,
        listId: '1'
      });
    });
  });

  describe('deleteItem', () => {
    test('должен вернуть экшен DELETE_ITEM', () => {
      expect(deleteItem('1', '2')).toEqual({
        type: ActionTypes.DELETE_ITEM,
        itemId: '1',
        listId: '2'
      });
    });
  });

  describe('deleteItemSuccess', () => {
    test('должен вернуть экшен DELETE_ITEM_SUCCESS', () => {
      expect(deleteItemSuccess('1', '2')).toEqual({
        type: ActionTypes.DELETE_ITEM_SUCCESS,
        itemId: '1',
        listId: '2'
      });
    });
  });

  describe('swapItems', () => {
    test('должен вернуть экшен SWAP_ITEMS', () => {
      const params = {
        itemId1: '1',
        itemId2: '1',
        listId: '1'
      };

      expect(swapItems(params)).toEqual({
        type: ActionTypes.SWAP_ITEMS,
        itemId1: params.itemId1,
        itemId2: params.itemId2,
        listId: params.listId
      });
    });
  });

  describe('reorderItems', () => {
    test('должен вернуть экшен REORDER_ITEMS', () => {
      expect(reorderItems('1')).toEqual({
        type: ActionTypes.REORDER_ITEMS,
        listId: '1'
      });
    });
  });

  describe('postpointDraftItemCreation', () => {
    test('должен вернуть экшен POSTPOINT_DRAFT_ITEM_CREATION', () => {
      expect(postpointDraftItemCreation('1')).toEqual({
        type: ActionTypes.POSTPOINT_DRAFT_ITEM_CREATION,
        listId: '1'
      });
    });
  });
});
