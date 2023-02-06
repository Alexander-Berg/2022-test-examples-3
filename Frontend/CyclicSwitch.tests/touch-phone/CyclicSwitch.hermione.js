describe('edu-components_CyclicSwitch_touch-phone', () => {
  it('should switch by tap', function() {
    return this.browser
      .openComponent('edu-components', 'cyclicswitch', 'playground')
      .assertView('plain', ['.Gemini'])
      .click('.Gemini .Test')
      .assertView('switched', ['.Gemini']);
  });
});
