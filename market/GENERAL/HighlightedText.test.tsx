import React from 'react';
import { render } from '@testing-library/react';

import { HighlightedText } from './HighlightedText';

describe('<HighlightedText/>', () => {
  it('split text to several highlighted parts', () => {
    const app = render(<HighlightedText text="Qwerty Qwertievich test Testik Testovich" searchString="test" />);
    const textparts = app.container.getElementsByTagName('span');
    expect(textparts).toHaveLength(7);
    expect(textparts[0].innerHTML).toBe('Qwerty Qwertievich ');
    expect(textparts[1].innerHTML).toBe('test');
    expect(textparts[2].innerHTML).toBe(' ');
    expect(textparts[3].innerHTML).toBe('Test');
    expect(textparts[4].innerHTML).toBe('ik ');
    expect(textparts[5].innerHTML).toBe('Test');
    expect(textparts[6].innerHTML).toBe('ovich');
  });

  it('text without match', () => {
    const app = render(<HighlightedText text="Qwerty Qwertievich" searchString="test" />);
    const textparts = app.container.getElementsByTagName('span');
    expect(textparts).toHaveLength(1);
  });

  it('empty search string', () => {
    const app = render(<HighlightedText text="Qwerty Qwertievich" />);
    const textparts = app.container.getElementsByTagName('span');
    expect(textparts).toHaveLength(0);
  });

  it('excapes regex special chars', () => {
    const app = render(<HighlightedText text="Qwerty .* Qwertievich" searchString=".*" />);
    const textparts = app.container.getElementsByTagName('span');
    expect(textparts).toHaveLength(3);
  });
});
