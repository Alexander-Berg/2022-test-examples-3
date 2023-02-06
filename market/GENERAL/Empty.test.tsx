import React from 'react';
import { render } from '@testing-library/react';

import { setupTestApp } from 'src/test/setupTestApp';
import { Empty } from './Empty';

describe('Empty::', () => {
  it('should render the component', () => {
    expect(() => {
      render(<Empty />);
    }).not.toThrow();
  });

  it('should not contain text', () => {
    const { app } = setupTestApp({ route: '/gwt' });
    expect(app.container.innerText).toBeFalsy();
  });
});
