describe('Номер страницы', () => {
  it('должен корректно отображаться', function() {
    return this.browser
      .openComponent('searchcontentpageinfo', 'default')
      .yaWaitForVisible('.news-search__page-info')
      .assertView('plain', '.news-search__page-info');
  });
});
