import { getErrors } from './utils';

import { validationResult } from 'src/test/data/validation';

describe('ShopModelValidationErrors utils', () => {
  test('getErrorWithoutParams', () => {
    const { errorAg, errorMt } = getErrors(validationResult);
    expect(errorAg.length).toEqual(2);
    expect(errorMt.length).toEqual(2);
  });
});
