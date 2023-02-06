import React from 'react';
import moment from 'moment';

import Todo from 'features/todo/components/Todo';

import {render, fireEvent, wait} from '../../helpers/test-utils';
import mockSuccessModelRequest from '../../helpers/mock-success-model-request';
import todo from '../../components/todo';
import base from '../../components/base';

describe('Тудушка', () => {
  describe('Выполненные дела', () => {
    beforeEach(() => {
      mockSuccessModelRequest('get-todo-sidebar', {
        items: [
          {
            type: 'todo-list',
            id: '2652093',
            title: 'test list',
            creationTs: Number(moment('2018-02-23T23:00'))
          },
          {
            type: 'todo-item',
            completed: false,
            uuid: '3871943',
            listId: '2652093',
            title: 'test item'
          }
        ],
        nextKeys: {}
      });
      mockSuccessModelRequest('do-update-todo-item');
    });

    test('cal-496: должен переместить дело в "Выполненные" ', async () => {
      const {getByText, queryByText, container} = render(<Todo />);

      await wait(() => getByText('test item'));

      fireEvent.click(container.querySelector(base.checkbox));

      await wait(() => expect(queryByText('test item')).toBeNull());

      fireEvent.click(container.querySelector(todo.tabCompleted));

      await wait(() => expect(getByText('test item')).toBeInTheDocument());
    });

    test('cal-503: должен активировать вкладку "Выполненные", только если есть выполненные дела', async () => {
      const {queryByText, getByText, container} = render(<Todo />);

      await wait(() => getByText('test item'));

      expect(container.querySelector(todo.tabCompleted)).toHaveClass(todo.tabDisabled);

      fireEvent.click(container.querySelector(base.checkbox));

      await wait(() => expect(queryByText('test item')).toBeNull());

      expect(container.querySelector(todo.tabCompleted)).not.toHaveClass(todo.tabDisabled);
    });

    test('должен открыть вкладку "Дела", если развыполнили последнее дело', async () => {
      const {getByText, container} = render(<Todo />);

      await wait(() => getByText('test item'));

      fireEvent.click(container.querySelector(base.checkbox));
      fireEvent.click(container.querySelector(todo.tabCompleted));

      await wait(() => expect(getByText('test item')).toBeInTheDocument());

      fireEvent.click(container.querySelector(base.checkbox));

      expect(container.querySelector(todo.tabCompleted)).toHaveClass(todo.tabDisabled);
      expect(container.querySelector(todo.tabAll)).toHaveClass(todo.tabActive);
    });
  });
});
