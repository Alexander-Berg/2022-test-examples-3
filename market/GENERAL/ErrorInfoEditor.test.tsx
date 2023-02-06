import React from 'react';
import { render } from '@testing-library/react';

import { ErrorInfoEditor } from './ErrorInfoEditor';

describe('<ErrorInfoEditor />', () => {
  it('renders without errors', async () => {
    const onChange = jest.fn();
    const app = render(<ErrorInfoEditor text="testik" source="" status="" isLoading onChange={onChange} />);

    expect(app.getByText('testik')).toBeTruthy();
  });
});
