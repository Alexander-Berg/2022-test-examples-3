describe('edu-components_CyclicSwitch_desktop', () => {
  it('should switch by click', function() {
    return this.browser
      .setViewportSize({ width: 600, height: 200 })
      .openComponent('edu-components', 'cyclicswitch', 'playground')
      .assertView('plain', ['.Gemini'])
      .click('.Gemini .Test')
      .assertView('switched', ['.Gemini']);
  });

  it('should switch with keyboard', function() {
    return (
      this.browser
        .setViewportSize({ width: 600, height: 200 })
        .openComponent('edu-components', 'cyclicswitch', 'playground')
        // для ie и ff
        .moveToObject('.Gemini', 0, 100)
        .buttonPress()
        // без клика окно в ie не получает фокус
        // клик должен быть ниже элемента в ff
        .keys('Tab')
        .keys('Space')
        .assertView('switched forward', ['.Gemini'])
        .keys('ArrowDown')
        .assertView('switched back', ['.Gemini'])
    );
  });
});
