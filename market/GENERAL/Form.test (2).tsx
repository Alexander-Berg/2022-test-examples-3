import { render } from '@testing-library/react';
import { Form } from 'react-final-form';
import React from 'react';

import { BaseSelectInput, NumberInput, TextInput } from './components';

describe('checks indexes', () => {
  it('renders all fields', () => {
    render(
      <Form
        onSubmit={() => undefined}
        render={({ ...formProps }) => (
          <form onSubmit={formProps.handleSubmit}>
            <TextInput name="textInput" />
            <NumberInput name="numberInput" />
            <BaseSelectInput name="selectInput" isMulti={false} />
            <BaseSelectInput name="multiSelectInput" isMulti />
          </form>
        )}
      />
    );
  });
});
