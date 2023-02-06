import { mount } from 'enzyme';
import React from 'react';
import { Form } from 'react-final-form';

import { TextArea } from './TextArea';

describe('TextArea', () => {
  it('main flow', () => {
    expect(() => mount(<Form onSubmit={() => undefined} render={() => <TextArea name="123" />} />)).not.toThrow();
  });
});
