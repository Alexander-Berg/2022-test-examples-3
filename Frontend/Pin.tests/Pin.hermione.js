['blueberry', 'grape'].forEach(flavor => {
  describe(`edu-components_Pin_${flavor}`, () => {
    const page = `atoms-pin-${flavor}`;

    it('should display default state', function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'default')
        .assertView('plain', ['.Gemini .Pin']);
    });
  });
});
