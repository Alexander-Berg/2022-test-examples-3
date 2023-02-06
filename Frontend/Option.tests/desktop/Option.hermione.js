const breakpoints = {
  mango: {
    small: 1024,
    middle: 1240,
    large: 1400,
  },
  blueberry: {
    small: 1024,
    middle: 1240,
    large: 1400,
  },
  pumpkin: {
    small: 360,
    middle: 1024,
    large: 1280,
  },
};

['mango', 'blueberry', 'whiskey', 'pumpkin', 'grape'].forEach(flavor => {
  describe(`edu-components_Option_${flavor}_desktop`, () => {
    const page = `atoms-option-${flavor}-desktop`;

    it('should display all states', function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'states')
        .assertView('plain', ['.Gemini']);
    });

    breakpoints[flavor] && it('should be adaptive', function() {
      const { small, middle, large } = breakpoints[flavor];

      return this.browser
        .openComponent('edu-components', page, 'states')
        .setViewportSize({ width: small, height: 768 })
        .assertView('small', ['.Option'])
        .setViewportSize({ width: middle, height: 768 })
        .assertView('middle', ['.Option'])
        .setViewportSize({ width: large, height: 768 })
        .assertView('large', ['.Option']);
    });

    it('should_visually_mark_element_on_hover', function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'states')
        .moveToObject('.Initial.NotChecked.Interactive')
        .assertView('hovered,_not_selected', ['.Initial.NotChecked.Interactive'])
        .moveToObject('.Initial.Checked.Interactive')
        .assertView('hovered,_selected', ['.Initial.Checked.Interactive']);
    });

    it('should NOT visually mark disabled element on hover', function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'states')
        .moveToObject('.Initial.NotChecked.Disabled')
        .assertView('hovered, not selected', ['.Initial.NotChecked.Disabled'])
        .moveToObject('.Initial.Checked.Disabled')
        .assertView('hovered, selected', ['.Initial.Checked.Disabled'])
        .moveToObject('.Checked.Correct')
        .assertView('hovered, selected, correct', ['.Checked.Correct'])
        .moveToObject('.Checked.Incorrect')
        .assertView('hovered, selected, incorrect', ['.Checked.Incorrect'])
        .moveToObject('.NotChecked.Correct')
        .assertView('hovered, not selected, correct', ['.NotChecked.Correct'])
        .moveToObject('.NotChecked.Incorrect')
        .assertView('hovered, not selected, incorrect', ['.NotChecked.Incorrect']);
    });

    it('should NOT visually mark readOnly element on hover', function() {
      return this.browser
        .setViewportSize({ width: 500, height: 500 })
        .openComponent('edu-components', page, 'states')
        .moveToObject('.Initial.NotChecked.Readonly')
        .assertView('hovered, not selected', ['.Initial.NotChecked.Readonly'])
        .moveToObject('.Initial.Checked.Readonly')
        .assertView('hovered, selected', ['.Initial.Checked.Readonly']);
    });
  });
});
