import React from 'react';
import { render } from '@testing-library/react';

import { Subject } from 'src/java/definitions';
import { ModalCreateCategoryRule } from './ModalCreateCategoryRule';

describe('<ModalCreateCategoryRule />', () => {
  it('renders without errors', () => {
    render(<ModalCreateCategoryRule parameters={[]} subject={Subject.PARAMETER} onSubmit={jest.fn()} />);
  });
});
