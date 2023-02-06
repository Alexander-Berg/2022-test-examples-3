describe('edu-components_Line_shapes', () => {
  const page = 'atoms-line-shapes';

  ['z', 'straight'].forEach(shape => {
    it(`should display shape ${shape}`, function() {
      return this.browser
        .setViewportSize({ width: 500, height: 500 })
        .openComponent('edu-components', page, shape)
        .assertView('plain', ['.Gemini']);
    });
  });
});

['blueberry', 'grape'].forEach(flavor => {
  describe(`edu-components_Line_${flavor}`, () => {
    const page = `atoms-line-${flavor}`;

    it('should display all states', function() {
      return this.browser
        .setViewportSize({ width: 500, height: 500 })
        .openComponent('edu-components', page, 'states')
        .assertView('plain', ['.Gemini']);
    });

    it('should visually mark element on hover', function() {
      return this.browser
        .setViewportSize({ width: 500, height: 500 })
        .openComponent('edu-components', page, 'states')
        .moveToObject('.Initial.Interactive .Line')
        .assertView('hovered', ['.Initial.Interactive']);
    });

    it('should NOT visually mark disabled element on hover', function() {
      return this.browser
        .setViewportSize({ width: 500, height: 500 })
        .openComponent('edu-components', page, 'states')
        .moveToObject('.Initial.Disabled .Line')
        .assertView('hovered', ['.Initial.Disabled']);
    });

    it('should NOT visually mark readOnly element on hover', function() {
      return this.browser
        .setViewportSize({ width: 500, height: 500 })
        .openComponent('edu-components', page, 'states')
        .moveToObject('.Initial.Readonly .Line')
        .assertView('hovered', ['.Initial.Readonly']);
    });
  });
});
