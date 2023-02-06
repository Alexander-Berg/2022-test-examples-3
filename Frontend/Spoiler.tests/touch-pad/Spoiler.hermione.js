['grape'].forEach(flavor => {
  describe(`edu-components_Spoiler_${flavor}_touch-pad`, () => {
    const page = `spoiler-${flavor}-touch-pad`;

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
