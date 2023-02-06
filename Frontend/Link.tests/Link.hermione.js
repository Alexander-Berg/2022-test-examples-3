describe('edu-components_Link_Lyceum', () => {
  it('themes', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'link-lyceum', 'themes')
      .moveToObject('body', -1, -1)
      .assertView('plain', ['.Gemini']);
  });

  it('normal theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'link-lyceum', 'themes')
      .moveToObject('.Link_theme_normal')
      .assertView('hovered', ['.Normal'])
      .moveToObject('.Link_theme_normal:last-child')
      .assertView('hovered disabled', ['.Normal']);
  });

  it('ghost theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'link-lyceum', 'themes')
      .moveToObject('.Link_theme_ghost')
      .assertView('hovered', ['.Ghost'])
      .moveToObject('.Link_theme_ghost:last-child')
      .assertView('hovered disabled', ['.Ghost']);
  });

  it('pseudo theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'link-lyceum', 'themes')
      .moveToObject('.Link_theme_pseudo')
      .assertView('hovered', ['.Pseudo'])
      .moveToObject('.Link_theme_pseudo:last-child')
      .assertView('hovered disabled', ['.Pseudo']);
  });
});

describe('edu-components_Link_Contest', () => {
  it('themes', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'link-contest', 'themes')
      .moveToObject('body', -1, -1)
      .assertView('plain', ['.Gemini']);
  });

  it('normal theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'link-contest', 'themes')
      .moveToObject('.Link_theme_normal')
      .assertView('hovered', ['.Normal'])
      .moveToObject('.Link_theme_normal:last-child')
      .assertView('hovered disabled', ['.Normal']);
  });

  it('black theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'link-contest', 'themes')
      .moveToObject('.Link_theme_black')
      .assertView('hovered', ['.Black'])
      .moveToObject('.Link_theme_black:last-child')
      .assertView('hovered disabled', ['.Black']);
  });

  it('ghost theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'link-contest', 'themes')
      .moveToObject('.Link_theme_ghost')
      .assertView('hovered', ['.Ghost'])
      .moveToObject('.Link_theme_ghost:last-child')
      .assertView('hovered disabled', ['.Ghost']);
  });

  it('outer theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'link-contest', 'themes')
      .moveToObject('.Link_theme_outer')
      .assertView('hovered', ['.Outer'])
      .moveToObject('.Link_theme_outer:last-child')
      .assertView('hovered disabled', ['.Outer']);
  });

  it('minor theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'link-contest', 'themes')
      .moveToObject('.Link_theme_minor')
      .assertView('hovered', ['.Minor'])
      .moveToObject('.Link_theme_minor:last-child')
      .assertView('hovered disabled', ['.Minor']);
  });

  it('pseudo theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'link-contest', 'themes')
      .moveToObject('.Link_theme_pseudo')
      .assertView('hovered', ['.Pseudo'])
      .moveToObject('.Link_theme_pseudo:last-child')
      .assertView('hovered disabled', ['.Pseudo']);
  });
});

describe('edu-components_Link_Pandora', () => {
  it('themes', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'link-pandora', 'themes')
      .moveToObject('body', -1, -1)
      .assertView('plain', ['.Gemini']);
  });

  it('normal theme', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'link-pandora', 'themes')
      .moveToObject('.Link_view_pandora')
      .assertView('hovered', ['.Normal'])
      .moveToObject('.Link_view_pandora:last-child')
      .assertView('hovered disabled', ['.Normal']);
  });
});
