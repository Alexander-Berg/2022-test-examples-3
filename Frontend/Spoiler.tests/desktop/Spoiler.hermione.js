['grape'].forEach(flavor => {
  describe(`edu-components_Spoiler_${flavor}_desktop`, () => {
    const page = `spoiler-${flavor}-desktop`;

    it('should toggle hidden content with a mouse', function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'default')
        .assertView('plain', ['.Gemini'])
        .moveToObject('.Gemini .Spoiler-Trigger')
        .assertView('hovered', ['.Gemini'])
        .buttonDown()
        .assertView('pressed', ['.Gemini'])
        .buttonUp()
        .assertView('open', ['.Gemini']);
    });

    it('should toggle hidden content with a keyboard', function() {
      return (
        this.browser
          .setViewportSize({ width: 1366, height: 768 })
          .openComponent('edu-components', page, 'default')
          // для ie и ff
          .moveToObject('.Gemini', 0, 500)
          .buttonPress()
          // без клика окно в ie не получает фокус
          // клик должен быть ниже элемента в ff
          .keys('Tab')
          .assertView('focused', ['.Gemini'])
          .keys('Enter')
          .assertView('open', ['.Gemini'])
          .keys('Space')
          .assertView('closed', ['.Gemini'])
      );
    });
  });
});
