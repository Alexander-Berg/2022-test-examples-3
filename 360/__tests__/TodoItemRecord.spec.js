import moment from 'moment';

import TodoItemRecord from '../TodoItemRecord';

describe('TodoItemRecord', () => {
  describe('isDraft', () => {
    test('должен возвращать false, если дело не является черновиком', () => {
      const todoItem = new TodoItemRecord({uuid: '1'});

      expect(todoItem.isDraft()).toBe(false);
    });

    test('должен возвращать true, если дело является черновиком', () => {
      const todoItem = new TodoItemRecord({uuid: 'draft-1'});

      expect(todoItem.isDraft()).toBe(true);
    });
  });

  describe('isExpired', () => {
    test('должен возвращать false, если у дела нет даты исполнения', () => {
      const todoItem = new TodoItemRecord();
      const currentDate = Number(moment('2017-01-30'));

      expect(todoItem.isExpired(currentDate)).toBe(false);
    });

    test('должен возвращать false, если дело завершенное', () => {
      const todoItem = new TodoItemRecord({dueDate: '2017-01-20', completed: true});
      const currentDate = Number(moment('2017-01-30'));

      expect(todoItem.isExpired(currentDate)).toBe(false);
    });

    test('должен возвращать false, если дело не является просроченным', () => {
      const todoItem = new TodoItemRecord({dueDate: '2017-01-20'});
      const currentDate = Number(moment('2017-01-10'));

      expect(todoItem.isExpired(currentDate)).toBe(false);
    });

    test('должен возвращать true, если дело является просроченным', () => {
      const todoItem = new TodoItemRecord({dueDate: '2017-01-20'});
      const currentDate = Number(moment('2017-01-30'));

      expect(todoItem.isExpired(currentDate)).toBe(true);
    });
  });
});
