import React from 'react';
import { Router } from 'react-router-dom';
import { render, screen } from '@testing-library/react';

import { history } from 'src/singletons';
import { Header } from './Header';

describe('Header', () => {
  it('renders', () => {
    render(
      <Router history={history}>
        <Header />
      </Router>
    );

    expect(screen.getByTitle('')).toBeInTheDocument();
  });
});
