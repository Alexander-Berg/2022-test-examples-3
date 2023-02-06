import escapeHTML from '../escapeHTML';

describe('escapeHTML', () => {
  it('dangerous characters', () => {
    expect(escapeHTML('<')).toBe('&lt;');
    expect(escapeHTML('>')).toBe('&gt;');
    expect(escapeHTML('&')).toBe('&amp;');
  });

  test('newline', () => {
    expect(escapeHTML('\n')).toBe('\n');
  });
});
