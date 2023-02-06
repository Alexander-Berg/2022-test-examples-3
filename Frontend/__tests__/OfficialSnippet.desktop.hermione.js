describe('Сниппет официального комментария', () => {
  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('должен отображаться корректно', function() {
    return this.browser
      .openComponent('officialsnippet', 'default', 'desktop')
      .setViewportSize({ width: 1366, height: 768 })
      .assertView('w-1366', '.mg-official-snippet')
      .setViewportSize({ width: 1600, height: 900 })
      .assertView('w-1600', '.mg-official-snippet');
  });
});
