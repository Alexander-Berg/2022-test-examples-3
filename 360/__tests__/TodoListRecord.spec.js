import TodoListRecord from '../TodoListRecord';

describe('TodoListRecord', () => {
  describe('isDraft', () => {
    test('должен возвращать false, если список не является черновиком', () => {
      const todoList = new TodoListRecord({id: '1'});

      expect(todoList.isDraft()).toBe(false);
    });

    test('должен возвращать true, если список является черновиком', () => {
      const todoList = new TodoListRecord({id: 'draft'});

      expect(todoList.isDraft()).toBe(true);
    });
  });
});
