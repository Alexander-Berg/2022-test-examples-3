import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider } from 'test/setupApp';
import { CONSTRUCTOR } from 'src/routes/routes';
import { CommonEntityTypeEnum } from 'src/java/definitions';
import { App } from 'src/App';
import { EntityCreatePage } from './EntityCreatePage';

describe('<EntityCreatePage/>', () => {
  it('renders without errors', () => {
    const { Provider } = setupTestProvider(
      `${CONSTRUCTOR.CREATE_ENTITY.path}/${CommonEntityTypeEnum.MDM_ATTR}?entityId=1`
    );
    expect(() => {
      render(
        <Provider>
          <App />
        </Provider>
      );
    }).not.toThrow();
  });
});

describe('EntityCreatePage', () => {
  it('renders', () => {
    const { Provider } = setupTestProvider();
    render(
      <Provider>
        <EntityCreatePage />
      </Provider>
    );
  });
});
