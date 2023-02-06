import React from 'react';
import moment from 'moment';

import i18n from 'utils/i18n';
import Todo from 'features/todo/components/Todo';

import {render, fireEvent, wait} from '../../helpers/test-utils';
import mockSuccessModelRequest from '../../helpers/mock-success-model-request';
import todo from '../../components/todo';
import modal from '../../components/modal';

describe('Тудушка', () => {
  describe('Удаление списка дел', () => {
    beforeEach(() => {
      mockSuccessModelRequest('do-delete-todo-list');
      mockSuccessModelRequest('do-update-user-settings');
    });

    test('cal-587: должен удалить пустой список дел', async () => {
      mockSuccessModelRequest('get-todo-sidebar', {
        items: [
          {
            type: 'todo-list',
            id: '2652093',
            title: 'test list',
            creationTs: Number(moment('2018-02-23T23:00'))
          }
        ],
        nextKeys: {}
      });

      const {container, getByText, queryByText} = render(<Todo />);

      await wait(() => getByText('test list'));

      fireEvent.click(container.querySelector(todo.listDelete));

      await wait(() => {
        expect(queryByText('test list')).toBeNull();
        expect(getByText(i18n.get('todo', 'empty'))).toBeInTheDocument();
      });
    });

    test('cal-505: должен удалить непустой список дел', async () => {
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

      const {container, getByText, queryByText} = render(<Todo />);

      await wait(() => getByText('test list'));

      fireEvent.click(container.querySelector(todo.listDelete));
      fireEvent.click(document.querySelector(modal.agree));

      await wait(() => {
        expect(queryByText('test list')).toBeNull();
        expect(queryByText('test item')).toBeNull();
        expect(getByText(i18n.get('todo', 'empty'))).toBeInTheDocument();
      });
    });
  });
});
