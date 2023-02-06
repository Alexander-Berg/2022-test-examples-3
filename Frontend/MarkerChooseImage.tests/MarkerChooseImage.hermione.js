['banana', 'blueberry'].forEach(flavor => {
  ['states', 'hasImage'].forEach(type => {
    describe(`edu-components_MarkerChooseImage_${flavor}_${type.toLowerCase()}`, () => {
      const page = `markerchooseimage-${flavor.toLowerCase()}`;

      it('should_display_all_states', function() {
        return this.browser
          .setViewportSize({ width: 1366, height: 768 })
          .openComponent('edu-components', page, type.toLowerCase())
          .assertView('plain', ['.Gemini'])
          .setViewportSize({ width: 640, height: 480 })
          .assertView('resized', ['.Gemini']);
      });

      it('should_NOT_visually_mark_readOnly_element_on_hover', function() {
        const selectedElement = '.Readonly .MarkerChooseImage-HitArea:nth-child(2)';
        const notSelectedElement = '.Readonly .MarkerChooseImage-HitArea:last-child';

        return this.browser
          .setViewportSize({ width: 1366, height: 768 })
          .openComponent('edu-components', page, type.toLowerCase())
          .pause(1000)
          .moveToObject(notSelectedElement)
          .assertView('hovered,not_selected', [notSelectedElement])
          .moveToObject(selectedElement)
          .assertView('hovered,selected', [selectedElement]);
      });

      it('should_NOT_visually_mark_disabled_element_on_hover', function() {
        const selectedElement = '.Disabled .MarkerChooseImage-HitArea:nth-child(2)';
        const notSelectedElement = '.Disabled .MarkerChooseImage-HitArea:last-child';

        return this.browser
          .setViewportSize({ width: 1366, height: 768 })
          .openComponent('edu-components', page, type.toLowerCase())
          .pause(1000)
          .moveToObject(notSelectedElement)
          .assertView('hovered,not_selected', [notSelectedElement])
          .moveToObject(selectedElement)
          .assertView('hovered,selected', [selectedElement]);
      });
    });
  });
});
