import React from 'react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { RewriteParametersControl, AboutRewrite } from './RewriteParametersControl';

describe('<RewriteParametersControl />', () => {
  test('render', () => {
    const onChange = jest.fn();
    const app = render(<RewriteParametersControl isRewrite onChange={onChange} />);
    const toggle = app.getByText('Перезаписать характеристики товаров');
    userEvent.click(toggle);
    expect(onChange).toBeCalled();
  });

  test('render AboutRewrite', () => {
    render(<AboutRewrite />);
  });
});
