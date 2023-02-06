import React from 'react';
import moment from 'moment';

import i18n from 'utils/i18n';
import KeyboardCodes from 'constants/KeyboardCodes';
import Todo from 'features/todo/components/Todo';

import {render, fireEvent, wait} from '../../helpers/test-utils';
import mockSuccessModelRequest from '../../helpers/mock-success-model-request';
import todo from '../../components/todo';
import base from '../../components/base';

describe('Тудушка', () => {
  describe('Просроченные дела', () => {
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
            title: 'test item',
            dueDate: moment()
              .add(-1, 'day')
              .format(moment.HTML5_FMT.DATE)
          }
        ],
        nextKeys: {}
      });
      mockSuccessModelRequest('get-holidays', {
        holidays: []
      });
      mockSuccessModelRequest('do-update-todo-item');
    });

    test('должен отображать просроченное дело во вкладках "Дела" и "Просроченные"', async () => {
      const {getByText, container} = render(<Todo />);

      await wait(() => {
        expect(getByText('test item')).toBeInTheDocument();
        expect(getByText(i18n.get('todo', 'dueDateYesterday'))).toBeInTheDocument();
        expect(container.querySelector(todo.tabExpired)).not.toHaveClass(todo.tabDisabled);
      });

      fireEvent.click(container.querySelector(todo.tabExpired));

      await wait(() => expect(getByText('test item')).toBeInTheDocument());
    });

    test('cal-498: должен возвращаться во вкладку "Дела", если чекнули последнее просроченное дело', async () => {
      const {getByText, container} = render(<Todo />);

      await wait(() => expect(getByText('test item')).toBeInTheDocument());

      fireEvent.click(container.querySelector(todo.tabExpired));

      await wait(() => expect(getByText('test item')).toBeInTheDocument());

      fireEvent.click(container.querySelector(base.checkbox));

      await wait(() => expect(container.querySelector(todo.tabAll)).toHaveClass(todo.tabActive));

      expect(container.querySelector(todo.tabExpired)).toHaveClass(todo.tabDisabled);
    });

    test('cal-513: должен возвращаться в "Дела", если поменяли дату последнего просроченного дела', async () => {
      const day = moment().format('DD.MM.YYYY');
      const {getByText, container} = render(<Todo />);

      await wait(() => expect(getByText('test item')).toBeInTheDocument());

      fireEvent.click(container.querySelector(todo.tabExpired));

      await wait(() => expect(getByText('test item')).toBeInTheDocument());

      fireEvent.click(container.querySelector(todo.itemEdit));
      fireEvent.change(container.querySelector(todo.itemDateInput), {target: {value: day}});
      fireEvent.keyDown(container.querySelector(todo.itemDateInput), {
        keyCode: KeyboardCodes.ENTER
      });

      expect(container.querySelector(todo.tabExpired)).toHaveClass(todo.tabDisabled);
      expect(container.querySelector(todo.tabAll)).toHaveClass(todo.tabActive);
    });
  });
});
