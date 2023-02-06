describe('edu-components_MarkerPolylines_touch-pad', () => {
  const page = 'markerpolylines-touch-pad';

  it('should display all states', function() {
    return this.browser
      .openComponent('edu-components', page, 'states')
      .assertView('states', ['.Gemini .MarkerPolylines']);
  });
});
