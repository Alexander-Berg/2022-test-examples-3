import React from 'react';
import { shallow } from 'enzyme';
import { Form } from 'react-final-form';

import { DatePicker } from './DatePicker';

describe('DatePicker', () => {
  it('main flow', () => {
    expect(() => shallow(<Form onSubmit={() => undefined} render={() => <DatePicker name="123" />} />)).not.toThrow();
  });
});
