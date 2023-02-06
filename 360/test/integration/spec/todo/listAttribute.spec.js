import React from 'react';
import moment from 'moment';

import Todo from 'features/todo/components/Todo';

import {render, wait} from '../../helpers/test-utils';
import mockSuccessModelRequest from '../../helpers/mock-success-model-request';
import todo from '../../components/todo';

describe('Тудушка', () => {
  test('cal-572: должен передать в title имя списка целиком', async () => {
    const longName = 'veryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName';

    mockSuccessModelRequest('get-todo-sidebar', {
      items: [
        {
          type: 'todo-list',
          id: '2652093',
          title: longName,
          creationTs: Number(moment('2018-02-23T23:00'))
        }
      ],
      nextKeys: {}
    });

    const {container} = render(<Todo />);

    await wait(() =>
      expect(container.querySelector(todo.listTitle)).toHaveAttribute('title', longName)
    );
  });
});
