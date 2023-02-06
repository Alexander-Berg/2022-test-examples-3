import React from 'react';

import { ClusterSearchForm } from 'src/widgets/ProductTreeVisualProperties/components/ClusterSearchForm/ClusterSearchForm';
import { renderWithProvider } from 'src/test/utils/utils';
import { setupTestProvider } from 'src/test/setupTestProvider';

describe('<ClusterSearchForm />', () => {
  it('renders without errors', () => {
    renderWithProvider(<ClusterSearchForm onSubmit={_ => undefined} />, setupTestProvider());
  });
});
