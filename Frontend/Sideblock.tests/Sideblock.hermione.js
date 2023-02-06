describe('edu-components_Sideblock', () => {
  it('should have fixed header', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'sideblock', 'fixed-header')
      .assertView('plain', ['.Gemini']);
  });

  it('should have blocking background', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'sideblock', 'blocking-background')
      .assertView('plain', ['.Gemini']);
  });

  it('should be side left', function() {
    return this.browser
      .setViewportSize({ width: 500, height: 500 })
      .openComponent('edu-components', 'sideblock', 'side-left')
      .assertView('plain', ['.Gemini']);
  });

  it('should be side top', function() {
    return this.browser
      .setViewportSize({ width: 500, height: 500 })
      .openComponent('edu-components', 'sideblock', 'side-top')
      .assertView('plain', ['.Gemini']);
  });

  it('should be side bottom', function() {
    return this.browser
      .setViewportSize({ width: 500, height: 500 })
      .openComponent('edu-components', 'sideblock', 'side-bottom')
      .assertView('plain', ['.Gemini']);
  });
});
