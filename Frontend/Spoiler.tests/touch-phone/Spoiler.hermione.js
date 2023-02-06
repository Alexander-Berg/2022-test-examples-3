['grape'].forEach(flavor => {
  describe(`edu-components_Spoiler_${flavor}_touch-phone`, () => {
    const page = `spoiler-${flavor}-touch-phone`;

    it('should toggle hidden content', function() {
      return this.browser
        .openComponent('edu-components', page, 'default')
        .assertView('plain', ['.Gemini'])
        .moveToObject('.Gemini .Spoiler-Caption')
        .buttonDown()
        .assertView('pressed', ['.Gemini'])
        .buttonUp()
        .assertView('open', ['.Gemini']);
    });
  });
});
