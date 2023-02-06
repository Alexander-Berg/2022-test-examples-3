import React from 'react';

import { CategoryRatingCell } from './CategoryRatingCell';
import { render } from '@testing-library/react';

describe('CategoryRatingCell', () => {
  test('render stat', () => {
    const app = render(<CategoryRatingCell rating={47} />);
    app.getByText('47%');
  });
});
