import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

describe('<Header /> in NY', () => {
  it('header renders in NY period', () => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date(1971, 0, 1, 12));

    // jest.setSystemTime should be called before reading Header module
    // eslint-disable-next-line @typescript-eslint/no-var-requires,global-require
    const { Header } = require('./Header');

    expect(() => {
      render(
        <MemoryRouter>
          <Header />
        </MemoryRouter>
      );
    }).not.toThrow();

    expect(screen.getByTitle('Жми')).toBeInTheDocument();
  });
});
