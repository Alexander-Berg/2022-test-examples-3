describe('Тело сюжета', () => {
  hermione.only.in(['appium-chrome-phone'], 'Достаточно проверки в одном браузере');
  it('должно корректно отображать нижнюю панель', function() {
    return this.browser
      .openComponent('Story', 'общий вид', 'phone')
      .yaWaitForVisible('.news-story__socials')
      .assertView('socials', '.news-story__socials')
      .click('.mg-story-menu__button')
      .assertView('menu', '.mg-story-menu__popup', { allowViewportOverflow: true })
      .orientation('landscape')
      .assertView('socials-landscape', '.news-story__socials');
  });
});
