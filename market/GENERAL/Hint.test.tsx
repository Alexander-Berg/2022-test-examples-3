import React from 'react';
import { render, fireEvent, act } from '@testing-library/react';

import { Hint } from './Hint';

describe('<Hint />', () => {
  it('renders without errors', () => {
    const app = render(<Hint text="Testik Testovich" />);
    const rootElement = app.container.getElementsByTagName('span')[0];
    expect(app.queryByText('Testik Testovich')).toBeNull();
    const hintIcon = app.container.getElementsByTagName('svg')[0];
    expect(hintIcon).toBeTruthy();
    act(() => {
      fireEvent.click(hintIcon);
    });
    expect(app.queryByText('Testik Testovich')).not.toBeNull();
    expect(rootElement.getAttribute('aria-describedby')).toBeTruthy();
    act(() => {
      fireEvent.click(hintIcon);
    });
    expect(rootElement.getAttribute('aria-describedby')).toBeFalsy();
  });
});
