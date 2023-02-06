import React from 'react';
import { render, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { EditableParam } from './EditableParam';
import { shopModel } from 'src/test/data';

describe('EditableParam', () => {
  test('edit params', async () => {
    const newValue = 'new name';
    const propsName = 'name';

    const onChange = jest.fn((key, value) => {
      // правильно ли приходят значения после редактирования
      expect(key).toEqual(propsName);
      expect(value).toEqual(newValue);
      return Promise.resolve([]);
    });

    const app = render(<EditableParam propKey={propsName} value={shopModel.name} onChangeParam={onChange} />);
    // активируем редактирование
    const editBtn = app.getByTitle('Редактировать значение');
    userEvent.click(editBtn);
    // вводим новое значение
    const input = app.getByRole('textbox');
    userEvent.clear(input);
    userEvent.type(input, newValue);
    // сохраняем
    const saveBtn = app.getByText(/сохранить/i);
    userEvent.click(saveBtn);

    await waitFor(() => {
      // перешел ли в режим просмотра после сохранения
      app.getByTitle('Редактировать значение');
    });
  });
});
