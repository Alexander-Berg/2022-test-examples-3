import React from 'react';
import { render } from '@testing-library/react';

import { ModalCreateCategoryPattern } from './ModalCreateCategoryPattern';

describe('<ModalCreateCategoryPattern />', () => {
  it('renders without errors', () => {
    render(<ModalCreateCategoryPattern parameters={[]} categoryHid={1} />);
  });
});
