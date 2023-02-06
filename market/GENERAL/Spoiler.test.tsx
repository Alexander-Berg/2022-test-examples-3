import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';

import { Spoiler } from './Spoiler';

describe('<Spoiler />', () => {
  const SPOILER_TEXT = 'Spoiler test title';
  const TEXT_UNDER_SPOILER = 'Text under spoiler';
  const getSpoilerHeader = () => screen.getByText(SPOILER_TEXT);

  beforeEach(() => {
    render(
      <Spoiler title={SPOILER_TEXT}>
        <div>{TEXT_UNDER_SPOILER}</div>
      </Spoiler>
    );
  });

  it('render without errors', () => {
    expect(getSpoilerHeader()).toBeInTheDocument();
    expect(screen.queryByText(TEXT_UNDER_SPOILER)).toBeNull();
  });

  it('spoiler shows content after clicking on header', () => {
    fireEvent.click(getSpoilerHeader());
    expect(screen.getByText(TEXT_UNDER_SPOILER)).toBeInTheDocument();
  });

  it('spoiler have class "Expanded" after clicking on header', () => {
    const spoilerHeader = getSpoilerHeader();
    fireEvent.click(spoilerHeader);
    expect(spoilerHeader).toHaveClass('Expanded');
  });
});
