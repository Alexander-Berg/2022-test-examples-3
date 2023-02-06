import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider } from 'test/setupApp';
import { App } from 'src/App';

describe('MdmMskuPage', () => {
  it('renders', () => {
    const { Provider } = setupTestProvider(`/mdm/msku`);
    const app = render(
      <Provider>
        <App />
      </Provider>
    );
    expect(app.getAllByText('MDM msku')).toHaveLength(1);
  });
});
