['blueberry', 'grape'].forEach(flavor => {
  describe(`edu-components_MarkerMacaroni_${flavor}_desktop`, () => {
    const page = `markermacaroni-${flavor}-desktop`;

    it('should_display_all_states', function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'states')
        .assertView('correct', ['.Gemini .Correct .MarkerMacaroni'])
        .assertView('incorrect', ['.Gemini .Incorrect .MarkerMacaroni'])
        .assertView('initial', ['.Gemini .Readonly .MarkerMacaroni']);
    });

    it('should_NOT_visually_mark_readOnly_element_on_hover', function() {
      const listItemElement =
        '.Readonly .MarkerMacaroni-List:first-child .MarkerMacaroni-ItemOption';
      const lineElement = '.Readonly .MarkerMacaroni-Line';

      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'states')
        .moveToObject(listItemElement)
        .assertView('hovered_list-item', ['.Readonly'])
        .moveToObject(lineElement)
        .assertView('hovered_line', ['.Readonly']);
    });

    it('should_NOT_visually_mark_disabled_element_on_hover', function() {
      const listItemElement =
        '.Disabled .MarkerMacaroni-List:first-child .MarkerMacaroni-ItemOption';
      const lineElement = '.Disabled .MarkerMacaroni-Line';

      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'states')
        .moveToObject(listItemElement)
        .assertView('hovered_list-item', ['.Readonly'])
        .moveToObject(lineElement)
        .assertView('hovered_line', ['.Readonly']);
    });

    it('should_connect_two_lists_with_a_mouse', function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'default')
        .assertView('plain', ['.Gemini .MarkerMacaroni'])
        .moveToObject('.Gemini .MarkerMacaroni-List_orientation_left .List-Item:first-child')
        .buttonPress()
        .assertView('pressed-left', ['.Gemini .MarkerMacaroni'])
        .moveToObject(
          '.Gemini .MarkerMacaroni-List_orientation_right .List-Item:first-child',
          -50,
          10,
        )
        .assertView('hovered-line', ['.Gemini .MarkerMacaroni'])
        .moveToObject('.Gemini .MarkerMacaroni-List_orientation_right .List-Item:first-child')
        .buttonPress()
        .assertView('pressed-right', ['.Gemini .MarkerMacaroni'])
        .moveToObject('.Gemini', -1, -1)
        .assertView('completed', ['.Gemini .MarkerMacaroni']);
    });

    it('should_remove_a_line_with_a_mouse', function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'default')
        .moveToObject('.Gemini .MarkerMacaroni-List_orientation_left .List-Item:first-child')
        .buttonPress()
        .moveToObject('.Gemini .MarkerMacaroni-List_orientation_right .List-Item:first-child')
        .buttonPress()
        .moveToObject('.Gemini', -1, -1)
        .assertView('completed', ['.Gemini .MarkerMacaroni'])
        .moveToObject('.Gemini .MarkerMacaroni-Line')
        .buttonPress()
        .moveToObject('.Gemini', -1, -1)
        .assertView('removed-line', ['.Gemini .MarkerMacaroni']);
    });

    it('should_remove_a_line_with_a_keyboard', function() {
      return (
        this.browser
          .setViewportSize({ width: 1366, height: 768 })
          .openComponent('edu-components', page, 'default')
          .moveToObject('.Gemini .MarkerMacaroni-List_orientation_left .List-Item:first-child')
          .buttonPress()
          .moveToObject('.Gemini .MarkerMacaroni-List_orientation_right .List-Item:first-child')
          .buttonPress()
          .moveToObject('.Gemini', -1, -1)
          .assertView('completed', ['.Gemini .MarkerMacaroni'])
          // для ie и ff
          .moveToObject('.Gemini', 0, 200)
          .buttonPress()
          .buttonPress()
          // без клика окно в ie не получает фокус
          // клик должен быть ниже элемента в ff
          .keys('Tab')
          .assertView('focused', ['.Gemini .MarkerMacaroni'])
          .keys('Enter')
          .assertView('removed-line', ['.Gemini .MarkerMacaroni'])
      );
    });

    it('should_interconnect_all_items', function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'default')
        .moveToObject('.Gemini .MarkerMacaroni-List_orientation_left .List-Item:first-child')
        .buttonPress()
        .moveToObject('.Gemini .MarkerMacaroni-List_orientation_right .List-Item:first-child')
        .buttonPress()
        .pause(200)
        .moveToObject('.Gemini .MarkerMacaroni-List_orientation_left .List-Item:first-child')
        .buttonPress()
        .moveToObject('.Gemini .MarkerMacaroni-List_orientation_right .List-Item:last-child')
        .buttonPress()
        .pause(200)
        .moveToObject('.Gemini .MarkerMacaroni-List_orientation_left .List-Item:last-child')
        .buttonPress()
        .moveToObject('.Gemini .MarkerMacaroni-List_orientation_right .List-Item:first-child')
        .buttonPress()
        .pause(200)
        .moveToObject('.Gemini .MarkerMacaroni-List_orientation_left .List-Item:last-child')
        .buttonPress()
        .moveToObject('.Gemini .MarkerMacaroni-List_orientation_right .List-Item:last-child')
        .buttonPress()
        .moveToObject('.Gemini', -1, -1)
        .assertView('completed', ['.Gemini .MarkerMacaroni']);
    });

    it('should_connect_two_lists_using_drag', function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'default')
        .assertView('plain', ['.Gemini .MarkerMacaroni'])
        .moveToObject('.Gemini .MarkerMacaroni-List_orientation_left .List-Item:first-child')
        .buttonDown()
        .assertView('pressed-left', ['.Gemini .MarkerMacaroni'])
        .moveToObject('.Gemini .MarkerMacaroni-List_orientation_right .List-Item:first-child')
        .assertView('hovered-right', ['.Gemini .MarkerMacaroni'])
        .buttonUp()
        .moveToObject('.Gemini', -1, -1)
        .assertView('completed', ['.Gemini .MarkerMacaroni']);
    });

    flavor === 'blueberry' && it('should_display_all_layouts', function() {
      return this.browser
        .setViewportSize({ width: 1366, height: 768 })
        .openComponent('edu-components', page, 'layouts')
        .assertView('line', ['.Gemini .Line .MarkerMacaroni']);
    });
  });
});
