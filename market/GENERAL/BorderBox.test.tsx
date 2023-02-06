import React from 'react';
import { render } from '@testing-library/react';
import { BorderBox } from './BorderBox';

test('<BorderBox />', () => {
  const app = render(
    <BorderBox>
      <span>BorderBox</span>
    </BorderBox>
  );

  app.getByText('BorderBox');
});
