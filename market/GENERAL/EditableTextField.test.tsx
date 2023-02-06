import React from 'react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { EditableTextField } from './EditableTextField';

describe('<EditableTextField />', () => {
  it('renders without errors', () => {
    const onChange = jest.fn();
    const app = render(<EditableTextField onChange={onChange} />);
    const editButton = app.getByTitle('Редактировать');
    userEvent.click(editButton);

    const textArea = app.container.getElementsByTagName('textarea').item(0);
    expect(textArea).toBeTruthy();

    const testString = 'Testik Testovich';
    userEvent.paste(textArea!, testString);
    const submitButton = app.getByText('Сохранить');
    userEvent.click(submitButton);

    expect(onChange).toBeCalledWith(testString);

    expect(app.queryByText('Сохранить')).toBeFalsy();
  });
});
