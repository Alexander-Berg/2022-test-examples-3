import React from 'react';
import { render } from '@testing-library/react';

import { InputModel } from 'src/utils/models';
import { SamplesEditor } from './SamplesEditor';

describe('<SamplesEditor />', () => {
  it('renders without errors', () => {
    render(<SamplesEditor parameters={[]} model={InputModel([])} />);
  });
});
