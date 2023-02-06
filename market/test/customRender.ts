// eslint-disable-next-line import/no-extraneous-dependencies
import { ReactElement } from 'react';
import { render, RenderOptions } from '@testing-library/react';

import { Wrapper } from './Wrapper';

const customRender = function customRender(ui: ReactElement, options: RenderOptions) {
  return render(ui, { wrapper: Wrapper, ...options });
} as typeof render;

export * from '@testing-library/react';
export { customRender as render };
