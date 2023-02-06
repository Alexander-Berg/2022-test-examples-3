describe('Баны поиска', () => {
  it('должны корректно отображаться', function() {
    return this.browser
      .openComponent('searchcontentbanmessage', 'rurkn')
      .yaWaitForVisible('.news-search__ban-message')
      .assertView('rurkn', '.news-search__ban-message')
      .openComponent('searchcontentbanmessage', 'ruoblivion')
      .yaWaitForVisible('.news-search__ban-message')
      .assertView('ruoblivion', '.news-search__ban-message')
      .openComponent('searchcontentbanmessage', 'memorandum')
      .yaWaitForVisible('.news-search__ban-message')
      .assertView('memorandum', '.news-search__ban-message')
      .openComponent('searchcontentbanmessage', 'merged')
      .yaWaitForVisible('.news-search__ban-message')
      .assertView('merged', '.news-search__ban-message');
  });
});
