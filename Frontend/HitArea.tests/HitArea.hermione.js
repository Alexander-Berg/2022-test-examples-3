['banana', 'blueberry'].forEach(flavor => {
  describe(`edu-components_HitArea_${flavor}`, () => {
    const page = `atoms-hitarea-${flavor}`;

    it('should display all states', function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'states')
        .assertView('plain', ['.Gemini']);
    });

    it('should_visually_mark_element_on_hover', function() {
      const selectedElement = '.Interactive .Checked';
      const notSelectedElement = '.Interactive .NotChecked';

      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'states')
        .scroll(notSelectedElement)
        .moveToObject(notSelectedElement)
        .assertView('hovered_not_selected', [notSelectedElement])
        .moveToObject(selectedElement)
        .assertView('hovered_selected', [selectedElement]);
    });

    it('should NOT visually mark readOnly element on hover', function() {
      const selectedElement = '.Readonly .Checked';
      const notSelectedElement = '.Readonly .NotChecked';

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
      const selectedElement = '.Disabled .Checked';
      const notSelectedElement = '.Disabled .NotChecked';

      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'states')
        .scroll(notSelectedElement)
        .moveToObject(notSelectedElement)
        .assertView('hovered, not selected', [notSelectedElement])
        .moveToObject(selectedElement)
        .assertView('hovered, selected', [selectedElement]);
    });

    flavor === 'blueberry' &&
      it('should align badge correctly', function() {
        return this.browser
          .setViewportSize({ width: 1366, height: 768 })
          .openComponent('edu-components', 'atoms-hitarea', 'badge-align')
          .waitForVisible('.Top-Left')
          .assertView('top left', ['.Top-Left .HitArea'])
          .waitForVisible('.Top-Right')
          .assertView('top right', ['.Top-Right .HitArea'])
          .waitForVisible('.Center')
          .assertView('center', ['.Center .HitArea'])
          .waitForVisible('.Bottom-Left')
          .assertView('bottom left', ['.Bottom-Left .HitArea'])
          .waitForVisible('.Bottom-Right')
          .assertView('bottom right', ['.Bottom-Right .HitArea']);
      });
  });
});
