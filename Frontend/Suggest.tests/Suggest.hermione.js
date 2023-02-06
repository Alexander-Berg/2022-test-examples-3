describe('edu-components_Suggest', () => {
  it('theme_normal', function() {
    return this.browser
      .openComponent('edu-components', 'suggest-theme', 'normal')
      .click('.Textinput-Control')
      .moveToObject('.Suggest-Item:nth-child(2)')
      .assertView('should-highlight-hovered-item', ['.Gemini'])
      .keys(['ArrowUp', 'ArrowDown', 'ArrowDown'])
      .assertView('should-highlight-selected-by-keyboard-item', ['.Gemini'])
      .click('body')
      .assertView('should-close-suggest-on-blur', ['.Gemini'])
      .click('.Textinput-Control')
      .assertView('should-open-suggest-on-focus', ['.Gemini'])
      .keys(['ArrowDown', 'ArrowDown', 'ArrowDown', 'ArrowDown', 'ArrowDown', 'ArrowDown'])
      .assertView('should-scroll-to-bottom-invisible-item', ['.Gemini'])
      .keys(['ArrowUp', 'ArrowUp', 'ArrowUp', 'ArrowUp', 'ArrowUp'])
      .assertView('should-scroll-to-top-invisible-item', ['.Gemini'])
      .keys(['Enter'])
      .assertView('should-close-suggest-on-item-select', ['.Gemini']);
  });
});
