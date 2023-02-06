import React from 'react';
import { render, screen } from '@testing-library/react';

import { Contacts } from './Contacts';
import { SOURCES_CONTACTS } from '../../entities/source';

describe('<Contacts />', () => {
  it('renders without errors', () => {
    expect(() =>
      render(
        <>
          {Object.values(SOURCES_CONTACTS).map((c, i) => (
            <Contacts key={i} {...c} links={i === 1 ? [{ link: 'testik', title: 'testovich' }] : undefined} />
          ))}
        </>
      )
    ).not.toThrow();

    expect(screen.getByText('testovich')).toBeTruthy();
  });
});
