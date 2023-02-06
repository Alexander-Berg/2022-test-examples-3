import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider } from 'test/setupApp';
import { App } from 'src/App';

describe('CategoryParamValuesPage', () => {
  it('renders', () => {
    const { Provider } = setupTestProvider('/mdm/category-param-values');
    const app = render(
      <Provider>
        <App />
      </Provider>
    );
    expect(app.getAllByText('Категорийные настройки')).toHaveLength(1);
  });
});
