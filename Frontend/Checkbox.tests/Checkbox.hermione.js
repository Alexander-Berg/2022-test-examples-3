describe('edu-components_Checkbox_Lyceum', () => {
  it('sizes', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'checkbox-lyceum', 'sizes')
      .assertView('plain', ['.Gemini']);
  });

  it('states', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'checkbox-lyceum', 'states')
      .moveToObject('body', -1, -1)
      .assertView('plain', ['.Gemini'])
      .moveToObject('.Checkbox:not(.Checkbox_checked)')
      .assertView('hovered not checked', ['.Gemini'])
      .moveToObject('.Checkbox.Checkbox_checked')
      .assertView('hovered checked', ['.Gemini'])
      .moveToObject('.Checkbox.Checkbox_disabled')
      .assertView('hovered disabled', ['.Gemini']);
  });
});
