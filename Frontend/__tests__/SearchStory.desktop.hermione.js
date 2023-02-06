describe('Поисковый сюжет', () => {
  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('должен отображаться корректно, когда он сюжет', function() {
    return this.browser
      .openComponent('searchstory', 'story', 'desktop')
      .yaWaitForVisible('.news-search-story__title')
      .setViewportSize({ width: 1600, height: 900 })
      .moveToObject('.news-search-story__title')
      .assertView('search-story-1600x900', '.news-search-story')
      .setViewportSize({ width: 1366, height: 768 })
      .moveToObject('.news-search-story__title')
      .assertView('search-story-1366x768', '.news-search-story')
      .click('.news-search-story__title')
      .pause(500)
      .back()
      .yaWaitForVisible('.mg-snippet__description')
      .moveToObject('.mg-snippet__description')
      .assertView('search-story-visited', '.news-search-story__title');
  });

  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('должен отображаться корректно, когда он однодокументник', function() {
    return this.browser
      .openComponent('searchstory', 'doc', 'desktop')
      .yaWaitForVisible('.mg-snippet__title')
      .setViewportSize({ width: 1600, height: 900 })
      .moveToObject('.mg-snippet__title')
      .assertView('search-doc-1600x900', '.news-search-story')
      .setViewportSize({ width: 1366, height: 768 })
      .moveToObject('.mg-snippet__title')
      .assertView('search-doc-1366x768', '.news-search-story');
  });
});
