import React from 'react';
import { render } from '@testing-library/react';
import { Form } from 'react-final-form';

import { SkuSettingsForm } from './SkuSettingsForm';
import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';

describe('<MainParametersForm />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <Form onSubmit={() => undefined} render={() => <SkuSettingsForm />} />
      </Provider>
    );
  });
});
