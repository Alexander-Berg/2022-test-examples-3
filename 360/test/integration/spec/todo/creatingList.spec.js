import React from 'react';
import moment from 'moment';

import i18n from 'utils/i18n';
import KeyboardCodes from 'constants/KeyboardCodes';
import Todo from 'features/todo/components/Todo';

import {render, fireEvent, wait} from '../../helpers/test-utils';
import mockSuccessModelRequest from '../../helpers/mock-success-model-request';
import todo from '../../components/todo';

describe('Тудушка', () => {
  describe('Создание списка дел', () => {
    beforeEach(() => {
      mockSuccessModelRequest('get-todo-sidebar', {
        items: [],
        nextKeys: {}
      });
      mockSuccessModelRequest('do-create-todo-list', {
        creationTs: Number(moment('2018-02-23T23:00')),
        listId: '100500'
      });
    });

    test('cal-482: должен создавать список дел при нажатии на Enter в поле ввода', async () => {
      const {getByText, container} = render(<Todo />);

      await wait(() => getByText(i18n.get('todo', 'empty')));

      fireEvent.click(container.querySelector(todo.listCreate));
      fireEvent.change(container.querySelector(todo.listTitleInput), {
        target: {
          value: 'new list'
        }
      });
      fireEvent.keyDown(container.querySelector(todo.listTitleInput), {
        keyCode: KeyboardCodes.ENTER
      });

      await wait(() => expect(getByText('new list')).toBeInTheDocument());
    });

    test('cal-555: должен создавать список дел при потере фокуса полем ввода', async () => {
      const {getByText, container} = render(<Todo />);

      await wait(() => getByText(i18n.get('todo', 'empty')));

      fireEvent.click(container.querySelector(todo.listCreate));
      fireEvent.change(container.querySelector(todo.listTitleInput), {
        target: {
          value: 'new list'
        }
      });
      fireEvent.blur(container.querySelector(todo.listTitleInput));

      await wait(() => expect(getByText('new list')).toBeInTheDocument());
    });
  });
});
