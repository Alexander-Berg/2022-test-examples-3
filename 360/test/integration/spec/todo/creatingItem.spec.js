import React from 'react';
import moment from 'moment';

import i18n from 'utils/i18n';
import KeyboardCodes from 'constants/KeyboardCodes';
import Todo from 'features/todo/components/Todo';

import {render, fireEvent, wait} from '../../helpers/test-utils';
import mockSuccessModelRequest from '../../helpers/mock-success-model-request';
import todo from '../../components/todo';
import datepicker from '../../components/datepicker';

describe('Тудушка', () => {
  describe('Создание дела', () => {
    beforeEach(() => {
      mockSuccessModelRequest('get-todo-sidebar', {
        items: [
          {
            type: 'todo-list',
            id: '100500',
            title: 'test list',
            creationTs: Number(moment('2018-02-23T23:00'))
          }
        ]
      });
      mockSuccessModelRequest('get-holidays', {
        holidays: []
      });
      mockSuccessModelRequest('do-create-todo-item', {
        'todo-item': {
          type: 'todo-item',
          uuid: '3866264',
          listId: '100500'
        }
      });
    });

    describe('без даты', () => {
      test('cal-499: должен создавать дело без даты при нажатии Enter в поле ввода', async () => {
        const {container, getByText} = render(<Todo />);

        await wait(() => getByText('test list'));

        fireEvent.click(container.querySelector(todo.itemCreate));
        fireEvent.change(container.querySelector(todo.itemTitleInput), {
          target: {value: 'new item'}
        });
        fireEvent.keyDown(container.querySelector(todo.itemTitleInput), {
          keyCode: KeyboardCodes.ENTER
        });

        await wait(() => expect(getByText('new item')).toBeInTheDocument());
      });

      test('cal-556: должен создавать дело без даты при потере фокуса полем ввода', async () => {
        const {container, getByText} = render(<Todo />);

        await wait(() => getByText('test list'));

        fireEvent.click(container.querySelector(todo.itemCreate));
        fireEvent.change(container.querySelector(todo.itemTitleInput), {
          target: {value: 'new item'}
        });
        fireEvent.blur(container.querySelector(todo.itemTitleInput));

        await wait(() => expect(getByText('new item')).toBeInTheDocument());
      });
    });

    describe('с датой', () => {
      test('cal-565: должен создавать дело c датой из datepicker при нажатии Enter в поле ввода даты', async () => {
        const {container, getByText} = render(<Todo />);

        await wait(() => getByText('test list'));

        fireEvent.click(container.querySelector(todo.itemCreate));
        fireEvent.change(container.querySelector(todo.itemTitleInput), {
          target: {value: 'new item'}
        });
        fireEvent.click(container.querySelector(todo.itemDateInput));
        fireEvent.click(container.querySelector(datepicker.today));
        fireEvent.keyDown(container.querySelector(todo.itemDateInput), {
          keyCode: KeyboardCodes.ENTER
        });

        await wait(() => expect(getByText(i18n.get('todo', 'dueDateToday'))).toBeInTheDocument());
      });
    });
  });
});
