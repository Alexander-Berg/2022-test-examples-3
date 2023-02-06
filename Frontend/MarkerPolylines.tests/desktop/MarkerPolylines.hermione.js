describe('edu-components_MarkerPolylines_desktop', () => {
  const page = 'markerpolylines-desktop';

  it('should display all states', function() {
    return this.browser
      .setViewportSize({ width: 1366, height: 768 })
      .openComponent('edu-components', page, 'states')
      .waitForVisible('.Gemini .MarkerPolylines')
      .assertView('states', ['.Gemini .MarkerPolylines']);
  });
});
