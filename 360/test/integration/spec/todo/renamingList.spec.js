import React from 'react';
import moment from 'moment';

import KeyboardCodes from 'constants/KeyboardCodes';
import Todo from 'features/todo/components/Todo';

import {render, fireEvent, wait} from '../../helpers/test-utils';
import mockSuccessModelRequest from '../../helpers/mock-success-model-request';
import todo from '../../components/todo';

describe('Тудушка', () => {
  describe('cal-561: Редактирование списка дел', () => {
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
      mockSuccessModelRequest('do-update-todo-list');
    });

    test('должен отредактировать список кликом по Enter', async () => {
      const {getByText, container} = render(<Todo />);

      await wait(() => getByText('test list'));

      fireEvent.click(container.querySelector(todo.listEdit));
      fireEvent.change(container.querySelector(todo.listTitleInput), {target: {value: 'new list'}});
      fireEvent.keyDown(container.querySelector(todo.listTitleInput), {
        keyCode: KeyboardCodes.ENTER
      });

      expect(getByText('new list')).toBeInTheDocument();
    });

    test('должен отредактировать список при потере фокуса полем ввода', async () => {
      const {getByText, container} = render(<Todo />);

      await wait(() => getByText('test list'));

      fireEvent.click(container.querySelector(todo.listEdit));
      fireEvent.change(container.querySelector(todo.listTitleInput), {target: {value: 'new list'}});
      fireEvent.blur(container.querySelector(todo.listTitleInput));

      expect(getByText('new list')).toBeInTheDocument();
    });

    test('не должен отредактировать список кликом по Esc', async () => {
      const {getByText, queryByText, container} = render(<Todo />);

      await wait(() => getByText('test list'));

      fireEvent.click(container.querySelector(todo.listEdit));
      fireEvent.change(container.querySelector(todo.listTitleInput), {target: {value: 'new list'}});
      fireEvent.keyDown(container.querySelector(todo.listTitleInput), {keyCode: KeyboardCodes.ESC});

      expect(queryByText('new list')).toBeNull();
      expect(getByText('test list')).toBeInTheDocument();
    });

    test('cal-504: должен показать инпут списка при клике по названию', async () => {
      const {getByText, container} = render(<Todo />);

      await wait(() => getByText('test list'));

      expect(container.querySelector(todo.listTitleInput)).not.toBeInTheDocument();

      fireEvent.click(container.querySelector(todo.listTitle));

      expect(container.querySelector(todo.listTitleInput)).toBeInTheDocument();
    });
  });
});
