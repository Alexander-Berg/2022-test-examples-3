describe('Топ-15(таблица)', () => {
  it('должна отображаться корректно', function() {
    return this.browser
      .openComponent('toptable', 'default')
      .click('.news-top-table__related-button')
      .assertView('plain', '.news-top-table', {
        allowViewportOverflow: true,
        captureElementFromTop: false,
        compositeImage: false,
      })
      .yaScroll('.news-top-table__stat-item:last-child')
      .assertView('right-part', '.news-top-table', {
        allowViewportOverflow: true,
        captureElementFromTop: false,
        compositeImage: false,
      });
  });

  hermione.only.notIn(['linux-chrome-iphone'], 'не работает setOrientation');
  it('должна отображаться корректно [landscape]', function() {
    return this.browser
      .openComponent('toptable', 'default')
      .setOrientation('landscape')
      .click('.news-top-table__related-button')
      .pause(1000)
      .assertView('landscape', '.news-top-table', {
        allowViewportOverflow: true,
        captureElementFromTop: false,
        compositeImage: false,
      });
  });
});
