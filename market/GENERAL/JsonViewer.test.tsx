import React from 'react';
import { render, screen } from '@testing-library/react';

import { JsonViewerWrapper } from './JsonViewer';

describe('<JsonViewer />', () => {
  it('renders without errors', () => {
    const jsonObject = {
      array: [1, 2, 3],
      subObj: {
        str: 'Testik',
        numb: 123,
        smth: undefined,
        nullable: null,
      },
      bool: false,
      smth: undefined,
      nullable: null,
    };

    render(<JsonViewerWrapper object={jsonObject} />);

    expect(screen.getAllByText('[', { exact: false })).toHaveLength(1);
    expect(screen.getAllByText(']', { exact: false })).toHaveLength(1);
    expect(screen.getByText('Testik', { exact: true })).toBeTruthy();
    expect(screen.getByText('123', { exact: true })).toBeTruthy();
  });
});
