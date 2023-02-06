import React from 'react';
import { render } from '@testing-library/react';

import { ParamOptionDto } from 'src/java/definitions';
import { ParentInfo } from './ParentInfo';

const parent = { valueId: 1, categoryId: 2 } as ParamOptionDto;
describe('<ParentInfo />', () => {
  it('renders without errors', () => {
    render(<ParentInfo parent={parent} index={0} />);
  });
});
