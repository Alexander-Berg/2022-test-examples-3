import TodoApi from '../TodoApi';

describe('TodoApi', () => {
  describe('getUncompletedItems', () => {
    test('должен отправлять запрос на получение дел', () => {
      const api = {
        post: jest.fn()
      };
      const todoApi = new TodoApi(api);

      todoApi.getUncompletedItems({
        dueFrom: 1,
        dueTo: 2
      });

      expect(api.post).toBeCalledWith('/get-uncompleted-todo-items', {
        dueFrom: 1,
        dueTo: 2
      });
    });
  });

  describe('getSidebarItems', () => {
    test('должен отправлять запрос на получение дел', () => {
      const api = {
        post: jest.fn()
      };
      const todoApi = new TodoApi(api);

      todoApi.getSidebarItems({
        withReminders: true
      });

      expect(api.post).toBeCalledWith('/get-todo-sidebar', {
        count: 20,
        withReminders: true
      });
    });
  });

  describe('createList', () => {
    test('должен отправлять запрос на создание списка', () => {
      const api = {
        post: jest.fn()
      };
      const todoApi = new TodoApi(api);

      todoApi.createList({
        title: 'new list'
      });

      expect(api.post).toBeCalledWith('/do-create-todo-list', {
        title: 'new list'
      });
    });
  });

  describe('editList', () => {
    test('должен отправлять запрос на редактирование списка', () => {
      const api = {
        post: jest.fn()
      };
      const todoApi = new TodoApi(api);

      todoApi.editList('100', {
        title: 'new title'
      });

      expect(api.post).toBeCalledWith('/do-update-todo-list', {
        'list-id': '100',
        title: 'new title'
      });
    });
  });

  describe('deleteList', () => {
    test('должен отправлять запрос на удаление списка', () => {
      const api = {
        post: jest.fn()
      };
      const todoApi = new TodoApi(api);

      todoApi.deleteList('100');

      expect(api.post).toBeCalledWith('/do-delete-todo-list', {
        'list-id': '100'
      });
    });
  });

  describe('createItem', () => {
    test('должен отправлять запрос на создание дела', () => {
      const api = {
        post: jest.fn()
      };
      const todoApi = new TodoApi(api);

      todoApi.createItem('100', {
        title: 'new item'
      });

      expect(api.post).toBeCalledWith('/do-create-todo-item', {
        'list-id': '100',
        title: 'new item'
      });
    });
  });

  describe('editItem', () => {
    test('должен отправлять запрос на редактирование дела', () => {
      const api = {
        post: jest.fn()
      };
      const todoApi = new TodoApi(api);

      todoApi.editItem('100', {
        title: 'new title'
      });

      expect(api.post).toBeCalledWith('/do-update-todo-item', {
        'todo-id': '100',
        title: 'new title'
      });
    });
  });

  describe('deleteItem', () => {
    test('должен отправлять запрос на удаление дела', () => {
      const api = {
        post: jest.fn()
      };
      const todoApi = new TodoApi(api);

      todoApi.deleteItem('100');

      expect(api.post).toBeCalledWith('/do-delete-todo-items', {
        'todo-ids': '100'
      });
    });
  });

  describe('reorderItems', () => {
    test('должен отправлять запрос на изменение порядка дел', () => {
      const api = {
        post: jest.fn()
      };
      const todoApi = new TodoApi(api);

      todoApi.reorderItems({
        listId: '100',
        itemsIds: '222,333,444',
        positions: '1,2,3'
      });

      expect(api.post).toBeCalledWith('/do-reorder-todo-items', {
        'list-id': '100',
        'todo-ids': '222,333,444',
        positions: '1,2,3'
      });
    });
  });
});
