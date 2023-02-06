describe('Выборы', () => {
  it('должны отображаться корректно', function() {
    return this.browser
      .openComponent('election', 'общий вид', 'phone')
      .assertView('default', '.news-election');
  });

  hermione.only.in('appium-chrome-phone', 'достаточно проверки в одном браузере');
  it('без данных по явке', function() {
    return this.browser
      .openComponent('election', 'без данных по явке', 'phone')
      .assertView('no-turnout', '.news-election');
  });

  hermione.only.in('appium-chrome-phone', 'достаточно проверки в одном браузере');
  it('без числа обработанных голосов', function() {
    return this.browser
      .openComponent('election', 'без числа обработанных голосов', 'phone')
      .assertView('no-processed-ballots', '.news-election');
  });

  hermione.only.in('appium-chrome-phone', 'достаточно проверки в одном браузере');
  it('без данных по явке и числа обработанных голосов', function() {
    return this.browser
      .openComponent('election', 'без данных по явке и числа обработанных голосов', 'phone')
      .assertView('no-turnout-and-no-processed-ballots', '.news-election');
  });
});
