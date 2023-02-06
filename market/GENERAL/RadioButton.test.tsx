import { mount } from 'enzyme';
import React from 'react';
import { Form } from 'react-final-form';

import { RadioButton } from './RadioButton';

describe('RadioButton', () => {
  it('main flow', () => {
    expect(() => mount(<Form onSubmit={() => undefined} render={() => <RadioButton name="123" />} />)).not.toThrow();
  });
});
