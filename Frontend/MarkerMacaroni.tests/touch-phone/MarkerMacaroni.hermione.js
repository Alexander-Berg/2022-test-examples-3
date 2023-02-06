['blueberry', 'grape'].forEach(flavor => {
  describe(`edu-components_MarkerMacaroni_${flavor}_touch-phone`, () => {
    const page = `markermacaroni-${flavor}-touch-phone`;

    it('should_display_all_states', function() {
      return this.browser
        .openComponent('edu-components', page, 'states')
        .assertView('correct', ['.Gemini .Correct .MarkerMacaroni'])
        .assertView('incorrect', ['.Gemini .Incorrect .MarkerMacaroni'])
        .assertView('initial', ['.Gemini .Readonly .MarkerMacaroni']);
    });

    it('should_connect_two_lists_with_the_taps', function() {
      return this.browser
        .openComponent('edu-components', page, 'default')
        .assertView('plain', ['.Gemini .MarkerMacaroni'])
        .touch('.Gemini .MarkerMacaroni-List_orientation_left .List-Item:first-child')
        .assertView('pressed-left', ['.Gemini .MarkerMacaroni'])
        .touch('.Gemini .MarkerMacaroni-List_orientation_right .List-Item:first-child')
        .assertView('completed', ['.Gemini .MarkerMacaroni']);
    });

    it('should_connect_two_lists_using_drag', async function() {
      await this.browser
        .openComponent('edu-components', page, 'default')
        .assertView('plain', ['.Gemini .MarkerMacaroni']);

      const { x: x1, y: y1 } = await this.browser.getLocation(
        '.Gemini .MarkerMacaroni-List_orientation_left .List-Item:first-child',
      );

      const intX1 = Math.floor(x1);
      const intY1 = Math.floor(y1);

      await this.browser
        .touchDown(intX1, intY1)
        .assertView('pressed-left', ['.Gemini .MarkerMacaroni']);

      const { x: x2, y: y2 } = await this.browser.getLocation(
        '.Gemini .MarkerMacaroni-List_orientation_right .List-Item:first-child',
      );

      const intX2 = Math.floor(x2);
      const intY2 = Math.floor(y2);

      return this.browser
        .touchMove(intX2 - 30, intY2)
        .assertView('hovered-line', ['.Gemini .MarkerMacaroni'])
        .touchUp(intX2, intY2)
        .moveToObject('.Gemini', -1, -1)
        .assertView('completed', ['.Gemini .MarkerMacaroni']);
    });

    it('should_activate_a_line_with_the_first_tap', function() {
      return (
        this.browser
          .openComponent('edu-components', page, 'default')
          .touch('.Gemini .MarkerMacaroni-List_orientation_left .List-Item:first-child')
          .touch('.Gemini .MarkerMacaroni-List_orientation_right .List-Item:first-child')
          .assertView('completed', ['.Gemini .MarkerMacaroni'])
          .touch('.Gemini .MarkerMacaroni-Line')
          .assertView('active-line', ['.Gemini .MarkerMacaroni'])
      );
    });

    it('should_remove_an_activated_line_with_the_second_tap', function() {
      return (
        this.browser
          .openComponent('edu-components', page, 'default')
          .touch('.Gemini .MarkerMacaroni-List_orientation_left .List-Item:first-child')
          .touch('.Gemini .MarkerMacaroni-List_orientation_right .List-Item:first-child')
          .assertView('completed', ['.Gemini .MarkerMacaroni'])
          .touch('.Gemini .MarkerMacaroni-Line')
          .touch('.Gemini .MarkerMacaroni-Line')
          .assertView('removed-line', ['.Gemini .MarkerMacaroni'])
      );
    });

    it('should_deactivate_a_line_with_the_tap_outside', function() {
      return (
        this.browser
          .openComponent('edu-components', page, 'default')
          .touch('.Gemini .MarkerMacaroni-List_orientation_left .List-Item:first-child')
          .touch('.Gemini .MarkerMacaroni-List_orientation_right .List-Item:first-child')
          .assertView('completed', ['.Gemini .MarkerMacaroni'])
          .touch('.Gemini .MarkerMacaroni-Line')
          .assertView('active-line', ['.Gemini .MarkerMacaroni'])
          .touchDown(0, 0)
          .assertView('deactivated-line', ['.Gemini .MarkerMacaroni'])
      );
    });

    flavor === 'blueberry' && it('should_display_all_layouts', function() {
      return this.browser
        .openComponent('edu-components', page, 'layouts')
        .assertView('line', ['.Gemini .Line .MarkerMacaroni']);
    });
  });
});
