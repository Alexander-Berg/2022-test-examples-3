describe('edu-components_StatusBadge', () => {
  it('should display all statuses', function() {
    return this.browser
      .setViewportSize({ width: 1366, height: 768 })
      .openComponent('edu-components', 'atoms-statusbadge', 'statuses')
      .assertView('plain', ['.Gemini']);
  });
});
