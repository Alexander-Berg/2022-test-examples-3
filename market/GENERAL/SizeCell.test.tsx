import React from 'react';
import { render } from '@testing-library/react';

import { SizeDto } from 'src/java/definitions';
import { setupTestProvider } from 'src/test/setupTestProvider';
import { SizeCell } from './SizeCell';

describe('<SizeCell />', () => {
  it('renders without errors', () => {
    const Provider = setupTestProvider();
    render(
      <Provider>
        <SizeCell
          size={{ size_name: 'test', category_ids: [123] } as SizeDto}
          onDelete={jest.fn()}
          onEdit={jest.fn()}
        />
      </Provider>
    );
  });
});
