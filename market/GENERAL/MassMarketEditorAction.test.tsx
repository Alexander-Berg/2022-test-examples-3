import React from 'react';
import { render } from '@testing-library/react';

import { parameter } from 'src/test/data';
import { MassMarketEditorAction, MASS_EDITOR_TITLE } from './MassMarketEditorAction';

describe('<MassMarketEditorAction />', () => {
  test('render', () => {
    const app = render(<MassMarketEditorAction parameter={parameter} />);

    app.getByTitle(MASS_EDITOR_TITLE);
  });
});
