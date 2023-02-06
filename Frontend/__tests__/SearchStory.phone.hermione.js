describe('Поисковый сюжет', () => {
  it('должен отображаться корректно, когда он сюжет', function() {
    return this.browser
      .openComponent('searchstory', 'story')
      .yaWaitForVisible('.news-search-story')
      .assertView('search-story', '.news-search-story')
      .click('.news-search-story__title')
      .pause(500)
      .back()
      .assertView('search-story-visited', '.news-search-story__title');
  });

  it('должен отображаться корректно, когда он однодокументник', function() {
    return this.browser
      .openComponent('searchstory', 'doc')
      .yaWaitForVisible('.news-search-story')
      .assertView('search-doc', '.news-search-story');
  });

  it('должен отображать заголовок корректно, когда источник и время не помещаются', function() {
    return this.browser
      .openComponent('searchstory', 'long-title-details')
      .yaWaitForVisible('.news-search-story')
      .assertView('long-title-details', '.news-search-story__details');
  });
});
