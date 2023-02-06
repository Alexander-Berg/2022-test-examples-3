import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider } from 'test/setupApp';
import { App } from 'src/App';

describe('RemainingShelfLifePage', () => {
  it('renders', () => {
    const { Provider } = setupTestProvider('/mdm/period-of-remaining-shelf-life');
    const app = render(
      <Provider>
        <App />
      </Provider>
    );
    expect(app.getAllByText('Остаточные сроки годности')).toHaveLength(1);
  });
});
