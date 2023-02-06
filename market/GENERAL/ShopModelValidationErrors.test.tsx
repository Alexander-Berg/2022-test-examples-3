import React from 'react';
import { render } from '@testing-library/react';

import { ShopModelValidationErrors } from './ShopModelValidationErrors';
import { validationResult } from 'src/test/data/validation';

describe('ShopModelValidationErrors', () => {
  test('show errors', () => {
    const app = render(<ShopModelValidationErrors validationResult={validationResult} />);
    app.getByText(new RegExp(validationResult.errors[0].message, 'i'));
    app.getByText(new RegExp(validationResult.errors[1].message, 'i'));
  });
});
