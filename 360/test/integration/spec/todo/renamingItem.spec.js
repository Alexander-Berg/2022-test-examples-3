import React from 'react';
import moment from 'moment';

import KeyboardCodes from 'constants/KeyboardCodes';
import Todo from 'features/todo/components/Todo';

import {render, fireEvent, wait} from '../../helpers/test-utils';
import mockSuccessModelRequest from '../../helpers/mock-success-model-request';
import todo from '../../components/todo';

describe('Тудушка', () => {
  describe('cal-869: Редактирование дела', () => {
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
      mockSuccessModelRequest('get-holidays', {
        holidays: []
      });
      mockSuccessModelRequest('do-update-todo-item');
    });

    test('должен отредактировать дело кликом по Enter', async () => {
      const {getByText, container} = render(<Todo />);

      await wait(() => getByText('test item'));

      fireEvent.click(container.querySelector(todo.itemEdit));
      fireEvent.change(container.querySelector(todo.itemTitleInput), {target: {value: 'new item'}});
      fireEvent.keyDown(container.querySelector(todo.itemTitleInput), {
        keyCode: KeyboardCodes.ENTER
      });

      expect(getByText('new item')).toBeInTheDocument();
    });

    test('должен отредактировать дело при потере фокуса полем ввода', async () => {
      const {getByText, container} = render(<Todo />);

      await wait(() => getByText('test item'));

      fireEvent.click(container.querySelector(todo.itemEdit));
      fireEvent.change(container.querySelector(todo.itemTitleInput), {target: {value: 'new item'}});
      fireEvent.blur(container.querySelector(todo.itemTitleInput));

      await wait(() => expect(getByText('new item')).toBeInTheDocument());
    });

    test('не должен отредактировать дело кликом по Esc', async () => {
      const {getByText, container} = render(<Todo />);

      await wait(() => getByText('test item'));

      fireEvent.click(container.querySelector(todo.itemEdit));
      fireEvent.change(container.querySelector(todo.itemTitleInput), {target: {value: 'new item'}});
      fireEvent.keyDown(container.querySelector(todo.itemTitleInput), {keyCode: KeyboardCodes.ESC});

      await wait(() => expect(getByText('test item')).toBeInTheDocument());
    });

    test('cal-561: должен показать инпут дела при клике по названию', async () => {
      const {getByText, container} = render(<Todo />);

      await wait(() => getByText('test item'));

      expect(container.querySelector(todo.itemTitleInput)).not.toBeInTheDocument();

      fireEvent.click(container.querySelector(todo.itemTitle));

      expect(container.querySelector(todo.itemTitleInput)).toBeInTheDocument();
    });
  });
});
