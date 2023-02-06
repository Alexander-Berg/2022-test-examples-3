import React from 'react';
import { render } from '@testing-library/react';

import { CategoryConfidenceCell } from './CategoryConfidenceCell';
import { shopModel } from 'src/test/data';
import { CategoryConfidence } from 'src/entities/category/types';
import { CATEGORY_CONFIDENCE_TITLES } from 'src/entities/category/constants';

const testSuits = [
  {
    model: { ...shopModel, marketCategoryChecked: true },
    status: CategoryConfidence.CHECKED,
  },
  {
    model: { ...shopModel, marketCategoryChecked: false, marketCategoryConfidence: 0.99 },
    status: CategoryConfidence.HIGHT,
  },
  {
    model: { ...shopModel, marketCategoryChecked: false, marketCategoryConfidence: 0.91 },
    status: CategoryConfidence.MEDIUM,
  },
  {
    model: { ...shopModel, marketCategoryChecked: false, marketCategoryConfidence: 0.5 },
    status: CategoryConfidence.BAD,
  },
];

testSuits.forEach(el => {
  test(`render CategoryConfidenceCell ${el.status} status`, () => {
    const app = render(<CategoryConfidenceCell row={el.model} />);
    app.getByText(CATEGORY_CONFIDENCE_TITLES[el.status]);
  });
});
