import React from 'react';
import { render } from '@testing-library/react';
import { Form } from 'react-final-form';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { MainParametersForm } from './MainParametersForm';

describe('<MainParametersForm />', () => {
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
            names: [],
          }}
          render={() => <MainParametersForm isGlobal />}
        />
      </Provider>
    );
  });
});
