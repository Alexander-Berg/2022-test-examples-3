import React from 'react';
import { render } from '@testing-library/react';

import TableUtils from '../../Table.utils';
import { ImagePicker } from './ImagePicker';
import { setupTestProvider } from 'src/test/setupTestProvider';

const data = TableUtils.getNewRowData(0);
describe('<ImagePicker />', () => {
  it('renders without errors', () => {
    const Provider = setupTestProvider();
    render(
      <Provider>
        <ImagePicker {...data} />
      </Provider>
    );
  });
});
