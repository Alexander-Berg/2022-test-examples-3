/* eslint-disable import/no-extraneous-dependencies */
import { render } from '@testing-library/react';

import { Wrapper } from './Wrapper';

const customRender = function customRender(ui, options) {
  return render(ui, { wrapper: Wrapper, ...options });
} as typeof render;

export * from '@testing-library/react';
export { customRender as render };
