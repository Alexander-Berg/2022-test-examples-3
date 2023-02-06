import { render } from '@testing-library/react';
import React from 'react';
import { Form } from 'react-final-form';

import { Checkbox } from './Checkbox';

describe('Checkbox', () => {
  it('main flow', () => {
    expect(() => render(<Form onSubmit={() => undefined} render={() => <Checkbox name="123" />} />)).not.toThrow();
  });

  it('with true value', () => {
    expect(() =>
      render(<Form initialValues={{ 123: true }} onSubmit={() => undefined} render={() => <Checkbox name="123" />} />)
    ).not.toThrow();
  });
});
