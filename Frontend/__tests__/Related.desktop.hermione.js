hermione.only.in(['linux-chrome'], 'Достаточно проверки в одном браузере');
describe('Блок-карусель "Ещё по теме"', () => {
  it('корректно отображается', function() {
    return this.browser
      .openComponent('related', 'default', 'desktop')
      .waitForVisible('.news-related__carousel')
      .assertView(
        'default',
        ['.news-related__carousel', '.mg-scroll__next'],
        { allowViewportOverflow: true },
      )
      .moveToObject('.news-related__carousel')
      .assertView(
        'hovered',
        ['.news-related__carousel', '.mg-scroll__next'],
        { allowViewportOverflow: true },
      )
      .moveToObject('.mg-scroll__next')
      .assertView(
        'button-hovered',
        ['.news-related__carousel', '.mg-scroll__next'],
        { allowViewportOverflow: true },
      )
      .click('.mg-scroll__next')
      .assertView(
        'next-slide',
        ['.news-related__carousel', '.mg-scroll__next', '.mg-scroll__prev'],
        { allowViewportOverflow: true },
      );
  });
});
