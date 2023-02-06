describe('Страница пустых результатов поиска', () => {
  it('должена отображаться корректно', function() {
    return this.browser
      .openComponent('emptysearch', 'default', 'phone')
      .yaWaitForVisible('.mg-layout-item')
      .assertView('empty-search', '.mg-layout-item', { allowViewportOverflow: true });
  });
});
