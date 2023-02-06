describe('Страница пустых результатов поиска', () => {
  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('должена отображаться корректно', function() {
    return this.browser
      .openComponent('emptysearch', 'default', 'desktop')
      .setViewportSize({ width: 480, height: 600 })
      .yaWaitForVisible('.mg-grid__item')
      .assertView('empty-search', '.mg-grid__item');
  });
});
