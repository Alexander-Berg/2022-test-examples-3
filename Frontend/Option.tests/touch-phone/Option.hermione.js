['mango', 'blueberry', 'whiskey', 'pumpkin', 'grape'].forEach(flavor => {
  describe(`edu-components_Option_${flavor}_touch-phone`, () => {
    const page = `atoms-option-${flavor}-touch-phone`;

    it('should display all states', function() {
      return this.browser
        .openComponent('edu-components', page, 'states')
        .assertView('plain', ['.Gemini']);
    });
  });
});
