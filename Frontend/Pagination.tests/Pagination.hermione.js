describe('edu-components_Pagination', () => {
  it('should display all possible states', function() {
    return this.browser
      .setViewportSize({ width: 800, height: 600 })
      .openComponent('edu-components', 'pagination', 'states')
      .assertView('plain', ['.Gemini'])
      .moveToObject('.Pagination-Item:first-child')
      .assertView('hovered', ['.Gemini']);
  });
});
