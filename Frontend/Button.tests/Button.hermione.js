describe('edu-components_Button_Lyceum', () => {
  it('sizes', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'button-lyceum', 'sizes')
      .assertView('plain', ['.Gemini']);
  });

  it('themes', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'button-lyceum', 'themes')
      .moveToObject('body', -1, -1)
      .assertView('plain', ['.Gemini']);
  });

  it('normal theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'button-lyceum', 'themes')
      .moveToObject('.Button2_theme_normal')
      .assertView('hovered', ['.Normal'])
      .buttonDown()
      .assertView('pressed', ['.Normal'])
      .buttonUp()
      .assertView('clicked', ['.Normal'])
      .moveToObject('.Button2_theme_normal:last-child')
      .assertView('hovered icon', ['.Normal']);
  });

  it('reject theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'button-lyceum', 'themes')
      .moveToObject('.Button2_theme_reject')
      .assertView('hovered', ['.Reject'])
      .buttonDown()
      .assertView('pressed', ['.Reject'])
      .buttonUp()
      .assertView('clicked', ['.Reject']);
  });

  it('pseudo theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'button-lyceum', 'themes')
      .moveToObject('.Button2_theme_pseudo')
      .assertView('hovered', ['.Pseudo'])
      .buttonDown()
      .assertView('pressed', ['.Pseudo'])
      .buttonUp()
      .assertView('clicked', ['.Pseudo'])
      .moveToObject('.Button2_theme_pseudo:last-child')
      .assertView('hovered icon', ['.Pseudo']);
  });

  it('action theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'button-lyceum', 'themes')
      .moveToObject('.Button2_theme_action')
      .assertView('hovered', ['.Action'])
      .buttonDown()
      .assertView('pressed', ['.Action'])
      .buttonUp()
      .assertView('clicked', ['.Action']);
  });

  it('approve theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'button-lyceum', 'themes')
      .moveToObject('.Button2_theme_approve')
      .assertView('hovered', ['.Approve'])
      .buttonDown()
      .assertView('pressed', ['.Approve'])
      .buttonUp()
      .assertView('clicked', ['.Approve']);
  });

  it('clear theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'button-lyceum', 'themes')
      .moveToObject('.Button2_theme_clear')
      .assertView('hovered', ['.Clear'])
      .buttonDown()
      .assertView('pressed', ['.Clear'])
      .buttonUp()
      .assertView('clicked', ['.Clear'])
      .moveToObject('.Button2_theme_clear:last-child')
      .assertView('hovered icon', ['.Clear']);
  });
});

describe('edu-components_Button_Pandora', () => {
  it('sizes', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'button-pandora', 'sizes')
      .assertView('plain', ['.Gemini']);
  });

  it('themes', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'button-pandora', 'themes')
      .moveToObject('body', -1, -1)
      .assertView('plain', ['.Gemini']);
  });

  it('none theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'button-pandora', 'themes')
      .moveToObject('.Button2_view_pandora')
      .assertView('hovered', ['.None'])
      .buttonDown()
      .assertView('pressed', ['.None'])
      .buttonUp()
      .assertView('clicked', ['.None'])
      .moveToObject('.Button2_view_pandora:last-child')
      .assertView('hovered icon', ['.None']);
  });

  it('purple-black theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'button-pandora', 'themes')
      .moveToObject('.Button2_theme_purple-black')
      .assertView('hovered', ['.PurpleBlack'])
      .buttonDown()
      .assertView('pressed', ['.PurpleBlack'])
      .buttonUp()
      .assertView('clicked', ['.PurpleBlack'])
      .moveToObject('.Button2_theme_purple-black:last-child')
      .assertView('hovered icon', ['.PurpleBlack']);
  });

  it('grape theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'button-pandora', 'themes')
      .moveToObject('.Button2_theme_grape')
      .assertView('hovered', ['.Grape'])
      .buttonDown()
      .assertView('pressed', ['.Grape'])
      .buttonUp()
      .assertView('clicked', ['.Grape'])
      .moveToObject('.Button2_theme_grape:last-child')
      .assertView('hovered icon', ['.Grape']);
  });

  it('grey theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'button-pandora', 'themes')
      .moveToObject('.Button2_theme_grey')
      .assertView('hovered', ['.Grey'])
      .buttonDown()
      .assertView('pressed', ['.Grey'])
      .buttonUp()
      .assertView('clicked', ['.Grey'])
      .moveToObject('.Button2_theme_grey:last-child')
      .assertView('hovered icon', ['.Grey']);
  });
});
