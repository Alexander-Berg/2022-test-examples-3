['mango', 'blueberry', 'whiskey', 'pumpkin'].forEach(flavor => {
  describe(`edu-components_MarkerChoice_${flavor}`, () => {
    const page = `markerchoice-${flavor}`;

    it('should display all states', function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'states')
        .assertView('plain', ['.Gemini']);
    });

    it('should NOT visually mark readOnly element on hover', function() {
      const selectedElement = '.Readonly .MarkerChoice-Option:first-child';
      const notSelectedElement = '.Readonly .MarkerChoice-Option:last-child';

      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'states')
        .scroll(notSelectedElement)
        .moveToObject(notSelectedElement)
        .assertView('hovered, not selected', [notSelectedElement])
        .moveToObject(selectedElement)
        .assertView('hovered, selected', [selectedElement]);
    });

    it('should NOT visually mark disabled element on hover', function() {
      const selectedElement = '.Disabled .MarkerChoice-Option:first-child';
      const notSelectedElement = '.Disabled .MarkerChoice-Option:last-child';

      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'states')
        .scroll(notSelectedElement)
        .moveToObject(notSelectedElement)
        .assertView('hovered, not selected', [notSelectedElement])
        .moveToObject(selectedElement)
        .assertView('hovered, selected', [selectedElement]);
    });

    it('should have horizontal layout', function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'layouts')
        .assertView('plain', ['.Horizontal']);
    });

    it('should have two column layout', function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'layouts')
        .assertView('plain', ['.Two-Cols']);
    });

    it('should have three column layout', function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'layouts')
        .assertView('plain', ['.Three-Cols']);
    });

    it('should have vertical layout', function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'layouts')
        .assertView('plain', ['.Vertical']);
    });
  });
});
