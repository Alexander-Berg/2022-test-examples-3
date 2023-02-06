describe('edu-components_MarkerPolylines_touch-phone', () => {
  const page = 'markerpolylines-touch-phone';

  it('should display all states', function() {
    return this.browser
      .openComponent('edu-components', page, 'states')
      .assertView('states', ['.Gemini .MarkerPolylines']);
  });
});
