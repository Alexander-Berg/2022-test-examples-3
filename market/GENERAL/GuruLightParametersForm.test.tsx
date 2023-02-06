import React from 'react';
import { Form } from 'react-final-form';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { GuruLightParametersForm } from './GuruLightParametersForm';

describe('<GuruLightParametersForm />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <Form
          onSubmit={() => undefined}
          initialValues={{
            aliases: [],
          }}
          render={() => <GuruLightParametersForm />}
        />
      </Provider>
    );
  });
});
