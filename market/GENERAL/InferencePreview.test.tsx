import React from 'react';
import { render } from '@testing-library/react';

import { InferencePreview } from './InferencePreview';

describe('<InferencePreview />', () => {
  it('render without errors', () => {
    render(<InferencePreview inferenceResults={[]} />);
  });
});
