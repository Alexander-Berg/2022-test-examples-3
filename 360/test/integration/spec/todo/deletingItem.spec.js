import React from 'react';
import moment from 'moment';

import Todo from 'features/todo/components/Todo';

import {render, fireEvent, wait} from '../../helpers/test-utils';
import mockSuccessModelRequest from '../../helpers/mock-success-model-request';
import todo from '../../components/todo';

describe('Тудушка', () => {
  test('cal-506: должен удалить дело из списка дел', async () => {
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
    mockSuccessModelRequest('do-delete-todo-items');

    const {container, getByText, queryByText} = render(<Todo />);

    await wait(() => getByText('test item'));

    fireEvent.click(container.querySelector(todo.itemDelete));

    await wait(() => expect(queryByText('test item')).toBeNull());
  });
});
