import {isEqual} from 'lodash';
import {
  put,
  call,
  select,
  all,
  takeEvery,
  takeLatest,
  setContext,
  getContext
} from 'redux-saga/effects';
import {cloneableGenerator} from 'redux-saga/utils';
import {Map} from 'immutable';
import {expectSaga} from 'redux-saga-test-plan';
import * as matchers from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';
import moment from 'moment';

import * as environment from 'configs/environment';
import SagaErrorReporter from 'utils/SagaErrorReporter';
import i18n from 'utils/i18n';
import {notificationHelpers, notifyFailure} from 'features/notifications/notificationsActions';

import TodoApi from '../TodoApi';
import {ActionTypes} from '../todoConstants';
import {getUncompletedItemsByListId} from '../todoSelectors';
import rootSaga, * as todoSagas from '../todoSagas';
import * as todoActions from '../todoActions';

const errorReporter = new SagaErrorReporter('todo');

jest.mock('configs/environment');

describe('todoSagas', () => {
  beforeEach(() => {
    jest.spyOn(notificationHelpers, 'generateId').mockReturnValue('id');
  });

  describe('rootSaga', () => {
    const gen = rootSaga();

    test('должен записать todoApi в контекст', () => {
      gen.next();
      expect(gen.next().value).toEqual(setContext({todoApi: new TodoApi()}));
    });
    test('должен подписаться на экшены', () => {
      expect(gen.next().value).toEqual(
        all([
          takeEvery(todoActions.loadTodosNetwork.type, todoSagas.loadTodosNetwork),
          takeEvery(todoActions.loadSidebarTodosNetwork.type, todoSagas.loadSidebarTodosNetwork),
          takeEvery(todoActions.loadSidebarTodosOffline.type, todoSagas.loadSidebarTodosOffline),
          takeEvery(
            todoActions.loadMoreSidebarTodosNetwork.type,
            todoSagas.loadMoreSidebarTodosNetwork
          ),
          takeEvery(
            todoActions.loadMoreSidebarTodosOffline.type,
            todoSagas.loadMoreSidebarTodosOffline
          ),
          takeEvery(ActionTypes.CREATE_LIST, todoSagas.createTodoList),
          takeEvery(ActionTypes.UPDATE_LIST, todoSagas.editTodoList),
          takeEvery(ActionTypes.DELETE_LIST, todoSagas.deleteTodoList),
          takeEvery(ActionTypes.CREATE_ITEM, todoSagas.createTodoItem),
          takeEvery(ActionTypes.UPDATE_ITEM, todoSagas.editTodoItem),
          takeEvery(ActionTypes.DELETE_ITEM, todoSagas.deleteTodoItem),
          takeEvery(ActionTypes.REORDER_ITEMS, todoSagas.reorderTodoItems),
          takeLatest(
            ActionTypes.POSTPOINT_DRAFT_ITEM_CREATION,
            todoSagas.postpointDraftItemCreation
          )
        ])
      );
    });
    test('должен завершить сагу', () => {
      expect(gen.next().done).toBe(true);
    });
  });

  describe('loadTodosNetwork', () => {
    describe('успешное выполнение', () => {
      it('должен делать запрос в api', () => {
        const todoApi = new TodoApi();
        const dueFrom = 1;
        const dueTo = 2;

        return expectSaga(todoSagas.loadTodosNetwork, {payload: {dueFrom, dueTo}})
          .provide([
            [matchers.getContext('todoApi'), todoApi],
            [matchers.call.fn(todoApi.getUncompletedItems)]
          ])
          .call([todoApi, todoApi.getUncompletedItems], {
            dueFrom: moment(dueFrom).format(moment.HTML5_FMT.DATE),
            dueTo: moment(dueTo)
              .add(1, 'day')
              .format(moment.HTML5_FMT.DATE)
          })
          .run();
      });

      it('должен записывать полученные дела в состояние', () => {
        const todoApi = new TodoApi();
        const dueFrom = 1;
        const dueTo = 2;
        const todos = [];

        return expectSaga(todoSagas.loadTodosNetwork, {payload: {dueFrom, dueTo}})
          .provide([
            [matchers.getContext('todoApi'), todoApi],
            [matchers.call.fn(todoApi.getUncompletedItems), todos]
          ])
          .put(todoActions.loadTodosSuccess(todos))
          .run();
      });

      it('должен сигнализировать о завершении', () => {
        const todoApi = new TodoApi();
        const dueFrom = 1;
        const dueTo = 2;
        const todos = [];

        return expectSaga(todoSagas.loadTodosNetwork, {payload: {dueFrom, dueTo}})
          .provide([
            [matchers.getContext('todoApi'), todoApi],
            [matchers.call.fn(todoApi.getUncompletedItems), todos]
          ])
          .put(todoActions.loadTodosDone({dueFrom, dueTo}))
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      it('должен логировать ошибку', () => {
        return expectSaga(todoSagas.loadTodosNetwork, {payload: {}})
          .provide([
            [matchers.getContext('todoApi'), throwError({name: 'error'})],
            [matchers.call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'loadTodosNetwork', {name: 'error'})
          .run();
      });

      it('должен сигнализировать о завершении', () => {
        const dueFrom = 1;
        const dueTo = 2;

        return expectSaga(todoSagas.loadTodosNetwork, {payload: {dueFrom, dueTo}})
          .provide([
            [matchers.getContext('todoApi'), throwError({name: 'error'})],
            [matchers.call.fn(errorReporter.send)]
          ])
          .put(todoActions.loadTodosDone({error: {name: 'error'}, dueFrom, dueTo}))
          .run();
      });
    });
  });

  describe('loadSidebarTodosNetwork', () => {
    const data = {
      todoApi: new TodoApi(),
      action: {
        payload: {
          withReminders: false,
          resolve() {}
        }
      },
      error: {
        name: 'error'
      }
    };
    data.gen = cloneableGenerator(todoSagas.loadSidebarTodosNetwork)(data.action);

    test('должен получить todoApi из контекста', () => {
      expect(data.gen.next().value).toEqual(getContext('todoApi'));
    });
    test('должен кинуть экшен loadSidebarTodosStart', () => {
      expect(data.gen.next(data.todoApi).value).toEqual(put(todoActions.loadSidebarTodosStart()));
    });
    test('должен сделать запрос на получение дел', () => {
      expect(data.gen.next().value).toEqual(
        call([data.todoApi, data.todoApi.getSidebarItems], data.action.payload)
      );
    });
    describe('success way', () => {
      beforeAll(() => {
        data.genSuccess = data.gen.clone();
      });
      test('должен кинуть экшен loadSidebarTodosSuccess', () => {
        expect(data.genSuccess.next({items: []}).value).toEqual(
          put(todoActions.loadSidebarTodosSuccess({items: []}))
        );
      });
      test('должен вызвать resolve', () => {
        expect(data.genSuccess.next().value).toEqual(call(data.action.payload.resolve));
      });
      test('должен завершить сагу', () => {
        expect(data.genSuccess.next().done).toBe(true);
      });
    });
    describe('failure way', () => {
      beforeAll(() => {
        data.genFailure = data.gen.clone();
      });
      test('должен залогировать ошибку', () => {
        expect(data.genFailure.throw(data.error).value).toEqual(
          call([errorReporter, errorReporter.send], 'loadSidebarTodosNetwork', data.error)
        );
      });
      test('должен кинуть экшн notifyFailure', () => {
        expect(data.genFailure.next().value).toEqual(
          put(notifyFailure({message: i18n.get('errors', 'loadTodosFailed')}))
        );
      });
      test('должен кинуть экшен loadSidebarTodosFailure', () => {
        expect(data.genFailure.next().value).toEqual(put(todoActions.loadSidebarTodosFailure()));
      });
      test('должен завершить сагу', () => {
        expect(data.genFailure.next().done).toBe(true);
      });
    });
  });

  describe('loadMoreSidebarTodosNetwork', () => {
    const data = {
      todoApi: new TodoApi(),
      action: {
        payload: {
          todosType: 'planned',
          nextKey: 'some-key',
          resolve() {}
        }
      },
      error: {
        name: 'error'
      }
    };
    data.gen = cloneableGenerator(todoSagas.loadMoreSidebarTodosNetwork)(data.action);

    test('должен получить todoApi из контекста', () => {
      expect(data.gen.next().value).toEqual(getContext('todoApi'));
    });
    test('должен сделать запрос на получение дел', () => {
      expect(data.gen.next(data.todoApi).value).toEqual(
        call([data.todoApi, data.todoApi.getSidebarItems], {
          ['next-' + data.action.payload.todosType]: data.action.payload.nextKey
        })
      );
    });
    describe('success way', () => {
      beforeAll(() => {
        data.genSuccess = data.gen.clone();
      });
      test('должен кинуть экшен loadMoreSidebarTodosSuccess', () => {
        expect(data.genSuccess.next({items: []}).value).toEqual(
          put(
            todoActions.loadMoreSidebarTodosSuccess({
              data: {items: []},
              todosType: data.action.payload.todosType
            })
          )
        );
      });
      test('должен вызвать resolve', () => {
        expect(data.genSuccess.next().value).toEqual(call(data.action.payload.resolve, false));
      });
      test('должен завершить сагу', () => {
        expect(data.genSuccess.next().done).toBe(true);
      });
    });
    describe('failure way', () => {
      beforeAll(() => {
        data.genFailure = data.gen.clone();
      });
      test('должен залогировать ошибку', () => {
        expect(data.genFailure.throw(data.error).value).toEqual(
          call([errorReporter, errorReporter.send], 'loadMoreSidebarTodosNetwork', data.error)
        );
      });
      test('должен кинуть экшн notifyFailure', () => {
        expect(data.genFailure.next().value).toEqual(
          put(notifyFailure({message: i18n.get('errors', 'loadTodosFailed')}))
        );
      });
      test('должен кинуть экшен loadMoreSidebarTodosFailure', () => {
        expect(data.genFailure.next().value).toEqual(
          put(todoActions.loadMoreSidebarTodosFailure())
        );
      });
      test('должен завершить сагу', () => {
        expect(data.genFailure.next().done).toBe(true);
      });
    });
  });

  describe('createTodoList', () => {
    const data = {
      todoApi: new TodoApi(),
      action: {
        params: {
          title: 'new todo list'
        }
      },
      error: {
        name: 'error'
      }
    };
    data.gen = cloneableGenerator(todoSagas.createTodoList)(data.action);

    test('должен получить todoApi из контекста', () => {
      expect(data.gen.next().value).toEqual(getContext('todoApi'));
    });
    test('должен сделать запрос на создание списка', () => {
      expect(data.gen.next(data.todoApi).value).toEqual(
        call([data.todoApi, data.todoApi.createList], data.action.params)
      );
    });
    describe('success way', () => {
      beforeAll(() => {
        data.genSuccess = data.gen.clone();
      });
      test('должен кинуть экшен createListSuccess', () => {
        const response = {
          listId: '1',
          creationTs: '123'
        };
        const newList = {
          ...data.action.params,
          id: response.listId,
          creationTs: response.creationTs
        };
        expect(data.genSuccess.next(response).value).toEqual(
          put(todoActions.createListSuccess(newList))
        );
      });
      test('должен завершить сагу', () => {
        expect(data.genSuccess.next().done).toBe(true);
      });
    });
    describe('failure way', () => {
      beforeAll(() => {
        data.genFailure = data.gen.clone();
      });
      test('должен залогировать ошибку', () => {
        expect(data.genFailure.throw(data.error).value).toEqual(
          call([errorReporter, errorReporter.send], 'createTodoList', data.error)
        );
      });
      test('должен кинуть экшен deleteDraftList', () => {
        expect(data.genFailure.next().value).toEqual(put(todoActions.deleteDraftList()));
      });
      test('должен кинуть экшен notifyFailure', () => {
        expect(data.genFailure.next().value).toEqual(put(notifyFailure({error: data.error})));
      });
      test('должен завершить сагу', () => {
        expect(data.genFailure.next().done).toBe(true);
      });
    });
  });

  describe('editTodoList', () => {
    const data = {
      todoApi: new TodoApi(),
      action: {
        id: '1',
        newParams: {
          title: 'new title'
        },
        oldParams: {
          title: 'old title'
        }
      },
      error: {
        name: 'error'
      }
    };
    data.gen = cloneableGenerator(todoSagas.editTodoList)(data.action);

    test('должен получить todoApi из контекста', () => {
      expect(data.gen.next().value).toEqual(getContext('todoApi'));
    });
    test('должен кинуть экшен editListSuccess', () => {
      expect(data.gen.next(data.todoApi).value).toEqual(
        put(
          todoActions.editListSuccess({
            id: data.action.id,
            params: data.action.newParams
          })
        )
      );
    });
    test('должен сделать запрос на редактирование списка', () => {
      expect(data.gen.next().value).toEqual(
        call([data.todoApi, data.todoApi.editList], data.action.id, data.action.newParams)
      );
    });
    describe('success way', () => {
      beforeAll(() => {
        data.genSuccess = data.gen.clone();
      });
      test('должен завершить сагу', () => {
        expect(data.genSuccess.next().done).toBe(true);
      });
    });
    describe('failure way', () => {
      beforeAll(() => {
        data.genFailure = data.gen.clone();
      });
      test('должен залогировать ошибку', () => {
        expect(data.genFailure.throw(data.error).value).toEqual(
          call([errorReporter, errorReporter.send], 'editTodoList', data.error)
        );
      });
      test('должен кинуть экшен editListFailure', () => {
        expect(data.genFailure.next().value).toEqual(
          put(
            todoActions.editListFailure({
              id: data.action.id,
              params: data.action.oldParams
            })
          )
        );
      });
      test('должен кинуть экшен notifyFailure', () => {
        expect(data.genFailure.next().value).toEqual(put(notifyFailure({error: data.error})));
      });
      test('должен завершить сагу', () => {
        expect(data.genFailure.next().done).toBe(true);
      });
    });
  });

  describe('deleteTodoList', () => {
    const data = {
      todoApi: new TodoApi(),
      action: {
        id: '1'
      },
      error: {
        name: 'error'
      }
    };
    data.gen = cloneableGenerator(todoSagas.deleteTodoList)(data.action);

    test('должен получить todoApi из контекста', () => {
      expect(data.gen.next().value).toEqual(getContext('todoApi'));
    });
    test('должен сделать запрос на удаление списка', () => {
      expect(data.gen.next(data.todoApi).value).toEqual(
        call([data.todoApi, data.todoApi.deleteList], data.action.id)
      );
    });
    describe('success way', () => {
      beforeAll(() => {
        data.genSuccess = data.gen.clone();
      });
      test('должен кинуть экшен deleteListSuccess', () => {
        expect(data.genSuccess.next().value).toEqual(
          put(todoActions.deleteListSuccess(data.action.id))
        );
      });
      test('должен завершить сагу', () => {
        expect(data.genSuccess.next().done).toBe(true);
      });
    });
    describe('failure way', () => {
      beforeAll(() => {
        data.genFailure = data.gen.clone();
      });
      test('должен залогировать ошибку', () => {
        expect(data.genFailure.throw(data.error).value).toEqual(
          call([errorReporter, errorReporter.send], 'deleteTodoList', data.error)
        );
      });
      test('должен кинуть экшен notifyFailure', () => {
        expect(data.genFailure.next().value).toEqual(put(notifyFailure({error: data.error})));
      });
      test('должен завершить сагу', () => {
        expect(data.genFailure.next().done).toBe(true);
      });
    });
  });

  describe('createTodoItem', () => {
    const data = {
      todoApi: new TodoApi(),
      action: {
        listId: '1',
        params: {
          title: 'new item'
        }
      },
      error: {
        name: 'error'
      }
    };
    data.gen = cloneableGenerator(todoSagas.createTodoItem)(data.action);

    test('должен получить todoApi из контекста', () => {
      expect(data.gen.next().value).toEqual(getContext('todoApi'));
    });
    test('должен сделать запрос на создание дела', () => {
      expect(data.gen.next(data.todoApi).value).toEqual(
        call([data.todoApi, data.todoApi.createItem], data.action.listId, data.action.params)
      );
    });
    describe('success way', () => {
      beforeAll(() => {
        data.genSuccess = data.gen.clone();
      });
      test('должен кинуть экшен createItemSuccess', () => {
        const response = {
          'todo-item': {
            uuid: '1',
            'list-id': '1'
          }
        };
        const newItem = {
          ...data.action.params,
          uuid: response['todo-item'].uuid,
          listId: response['todo-item'].listId
        };
        expect(data.genSuccess.next(response).value).toEqual(
          put(todoActions.createItemSuccess(newItem))
        );
      });
      test('должен проверить создано ли событие через Enter', () => {
        expect(data.genSuccess.next().value).toEqual(call(isEqual, data.action.enter, true));
      });
      describe('событие создано через Enter и не тач версия', () => {
        beforeAll(() => {
          data.genSuccessByEnter = data.genSuccess.clone();
          sinon.stub(environment, 'isTouch').value(false);
        });
        test('должен кинуть экшен createDraftItem', () => {
          expect(data.genSuccessByEnter.next(true).value).toEqual(
            put(todoActions.createDraftItem(data.action.listId))
          );
        });
        test('должен завершить сагу', () => {
          expect(data.genSuccessByEnter.next().done).toBe(true);
        });
      });
      describe('событие не создано через Enter', () => {
        beforeAll(() => {
          data.genSuccessNotByEnter = data.genSuccess.clone();
          sinon.stub(environment, 'isTouch').value(false);
        });
        test('должен завершить сагу', () => {
          expect(data.genSuccessNotByEnter.next(false).done).toBe(true);
        });
      });
      describe('тач версия', () => {
        beforeAll(() => {
          data.genSuccessTouch = data.genSuccess.clone();
          sinon.stub(environment, 'isTouch').value(true);
        });
        test('должен завершить сагу', () => {
          expect(data.genSuccessTouch.next(true).done).toBe(true);
        });
      });
    });
    describe('failure way', () => {
      beforeAll(() => {
        data.genFailure = data.gen.clone();
      });
      test('должен залогировать ошибку', () => {
        expect(data.genFailure.throw(data.error).value).toEqual(
          call([errorReporter, errorReporter.send], 'createTodoItem', data.error)
        );
      });
      test('должен кинуть экшен deleteDraftItem', () => {
        expect(data.genFailure.next().value).toEqual(
          put(todoActions.deleteDraftItem(data.action.listId))
        );
      });
      test('должен кинуть экшен notifyFailure', () => {
        expect(data.genFailure.next().value).toEqual(put(notifyFailure({error: data.error})));
      });
      test('должен завершить сагу', () => {
        expect(data.genFailure.next().done).toBe(true);
      });
    });
  });

  describe('editTodoItem', () => {
    const data = {
      todoApi: new TodoApi(),
      action: {
        itemId: '1',
        listId: '1',
        newParams: {
          title: 'new title'
        },
        oldParams: {
          title: 'old title'
        }
      },
      error: {
        name: 'error'
      }
    };
    data.gen = cloneableGenerator(todoSagas.editTodoItem)(data.action);

    test('должен получить todoApi из контекста', () => {
      expect(data.gen.next().value).toEqual(getContext('todoApi'));
    });
    test('должен кинуть экшен editItemSuccess', () => {
      expect(data.gen.next(data.todoApi).value).toEqual(
        put(
          todoActions.editItemSuccess({
            itemId: data.action.itemId,
            listId: data.action.listId,
            params: data.action.newParams
          })
        )
      );
    });
    test('должен сделать запрос на редактирование дела', () => {
      expect(data.gen.next().value).toEqual(
        call([data.todoApi, data.todoApi.editItem], data.action.itemId, data.action.newParams)
      );
    });
    describe('success way', () => {
      beforeAll(() => {
        data.genSuccess = data.gen.clone();
      });
      test('должен завершить сагу', () => {
        expect(data.genSuccess.next().done).toBe(true);
      });
    });
    describe('failure way', () => {
      beforeAll(() => {
        data.genFailure = data.gen.clone();
      });
      test('должен залогировать ошибку', () => {
        expect(data.genFailure.throw(data.error).value).toEqual(
          call([errorReporter, errorReporter.send], 'editTodoItem', data.error)
        );
      });
      test('должен кинуть экшен editItemFailure', () => {
        expect(data.genFailure.next().value).toEqual(
          put(
            todoActions.editItemFailure({
              itemId: data.action.itemId,
              listId: data.action.listId,
              params: data.action.oldParams
            })
          )
        );
      });
      test('должен кинуть экшен notifyFailure', () => {
        expect(data.genFailure.next().value).toEqual(put(notifyFailure({error: data.error})));
      });
      test('должен завершить сагу', () => {
        expect(data.genFailure.next().done).toBe(true);
      });
    });
  });

  describe('deleteTodoItem', () => {
    const data = {
      todoApi: new TodoApi(),
      action: {
        itemId: '1'
      },
      error: {
        name: 'error'
      }
    };
    data.gen = cloneableGenerator(todoSagas.deleteTodoItem)(data.action);

    test('должен получить todoApi из контекста', () => {
      expect(data.gen.next().value).toEqual(getContext('todoApi'));
    });
    test('должен сделать запрос на удаление дела', () => {
      expect(data.gen.next(data.todoApi).value).toEqual(
        call([data.todoApi, data.todoApi.deleteItem], data.action.itemId)
      );
    });
    describe('success way', () => {
      beforeAll(() => {
        data.genSuccess = data.gen.clone();
      });
      test('должен кинуть экшен deleteItemSuccess', () => {
        expect(data.genSuccess.next().value).toEqual(
          put(todoActions.deleteItemSuccess(data.action.itemId, data.action.listId))
        );
      });
      test('должен завершить сагу', () => {
        expect(data.genSuccess.next().done).toBe(true);
      });
    });
    describe('failure way', () => {
      beforeAll(() => {
        data.genFailure = data.gen.clone();
      });
      test('должен залогировать ошибку', () => {
        expect(data.genFailure.throw(data.error).value).toEqual(
          call([errorReporter, errorReporter.send], 'deleteTodoItem', data.error)
        );
      });
      test('должен кинуть экшен notifyFailure', () => {
        expect(data.genFailure.next().value).toEqual(put(notifyFailure({error: data.error})));
      });
      test('должен завершить сагу', () => {
        expect(data.genFailure.next().done).toBe(true);
      });
    });
  });

  describe('reorderTodoItems', () => {
    describe('нет незавершенных дел в списке', () => {
      const data = {
        action: {
          listId: '1'
        },
        error: {
          name: 'error'
        }
      };
      data.gen = todoSagas.reorderTodoItems(data.action);

      test('должен попытаться найти незавершенные дела по listId', () => {
        expect(data.gen.next().value).toEqual(
          select(getUncompletedItemsByListId, data.action.listId)
        );
      });
      test('должен завершить сагу', () => {
        expect(data.gen.next([]).done).toBe(true);
      });
    });
    describe('есть незавершенные дела в списке', () => {
      const data = {
        todoApi: new TodoApi(),
        action: {
          listId: '1'
        },
        items: new Map({
          '1': {uuid: '1', position: '2'},
          '2': {uuid: '2', position: '1'}
        }),
        error: {
          name: 'error'
        }
      };
      data.gen = cloneableGenerator(todoSagas.reorderTodoItems)(data.action);

      test('должен попытаться найти незавершенные дела по listId', () => {
        expect(data.gen.next().value).toEqual(
          select(getUncompletedItemsByListId, data.action.listId)
        );
      });
      test('должен получить todoApi из контекста', () => {
        expect(data.gen.next(data.items).value).toEqual(getContext('todoApi'));
      });
      test('должен сделать запрос на изменение порядка дел', () => {
        expect(data.gen.next(data.todoApi).value).toEqual(
          call([data.todoApi, data.todoApi.reorderItems], {
            listId: data.action.listId,
            itemsIds: '1,2',
            positions: '2,1'
          })
        );
      });
      describe('success way', () => {
        beforeAll(() => {
          data.genSuccess = data.gen.clone();
        });
        test('должен завершить сагу', () => {
          expect(data.genSuccess.next().done).toBe(true);
        });
      });
      describe('failure way', () => {
        beforeAll(() => {
          data.genFailure = data.gen.clone();
        });
        test('должен залогировать ошибку', () => {
          expect(data.genFailure.throw(data.error).value).toEqual(
            call([errorReporter, errorReporter.send], 'reorderTodoItems', data.error)
          );
        });
        test('должен кинуть экшен notifyFailure', () => {
          expect(data.genFailure.next().value).toEqual(put(notifyFailure({error: data.error})));
        });
        test('должен завершить сагу', () => {
          expect(data.genFailure.next().done).toBe(true);
        });
      });
    });
  });

  describe('postpointDraftItemCreation', () => {
    test('должен дожидаться сохранения уже создаваемого дела', () => {
      return expectSaga(todoSagas.postpointDraftItemCreation, {listId: 1})
        .not.put(todoActions.postpointDraftItemCreation(1))
        .run();
    });
    test('должен создавать новый черновик дела', () => {
      return expectSaga(todoSagas.postpointDraftItemCreation, {listId: 1})
        .dispatch({type: ActionTypes.CREATE_ITEM_SUCCESS})
        .put(todoActions.createDraftItem(1))
        .run();
    });
  });
});
