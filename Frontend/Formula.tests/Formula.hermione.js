describe('edu-components_Formula', () => {
  it('should display all states', function() {
    return this.browser
      .setViewportSize({ width: 600, height: 200 })
      .openComponent('edu-components', 'formula', 'states')
      .assertView('inline', ['.Gemini .Inline'])
      .assertView('multiline', ['.Gemini .Multiline'])
      .assertView('cyrillic', ['.Gemini .Cyrillic']);
  });
});
