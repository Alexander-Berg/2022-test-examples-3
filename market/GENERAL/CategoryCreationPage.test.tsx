import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider } from 'test/setupApp';
import { ARM_DS } from 'src/routes/routes';
import { App } from 'src/App';
import { CategoryCreatePage } from './CategoryCreatePage';

describe('<CategoryCreatePage/>', () => {
  it('renders without errors', () => {
    const { Provider } = setupTestProvider(`${ARM_DS.CREATE_CATEGORY.base}/34657/123123`);
    expect(() => {
      render(
        <Provider>
          <App />
        </Provider>
      );
    }).not.toThrow();
  });
});

describe('CategoryCreatePage', () => {
  it('renders', () => {
    const { Provider } = setupTestProvider();
    render(
      <Provider>
        <CategoryCreatePage />
      </Provider>
    );
  });
});
