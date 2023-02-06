import React from 'react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Toggler } from './Toggler';

describe('Toggler', () => {
  test('render', () => {
    const onChange = jest.fn((value: boolean) => {
      expect(value).toBeFalsy();
    });
    const app = render(<Toggler value onChange={onChange} texts={['On', 'Of']} />);

    userEvent.click(app.getByText('Of'));
  });
});
