hermione.only.notIn(['win-ie11'], 'статика для IE локализована, прогон тестов неактуален');
describe('Выборы', () => {
  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('должны отображаться корректно', function() {
    return this.browser
      .openComponent('election', 'общий вид', 'desktop')
      .setViewportSize({ width: 648, height: 600 })
      .assertView('default', '.news-election');
  });

  hermione.only.in('linux-chrome', 'достаточно проверки в одном браузере');
  it('без данных по явке', function() {
    return this.browser
      .openComponent('election', 'без данных по явке', 'desktop')
      .setViewportSize({ width: 648, height: 600 })
      .assertView('no-turnout', '.news-election');
  });

  hermione.only.in('linux-chrome', 'достаточно проверки в одном браузере');
  it('без числа обработанных голосов', function() {
    return this.browser
      .openComponent('election', 'без числа обработанных голосов', 'desktop')
      .setViewportSize({ width: 648, height: 600 })
      .assertView('no-processed-ballots', '.news-election');
  });

  hermione.only.in('linux-chrome', 'достаточно проверки в одном браузере');
  it('без данных по явке и числа обработанных голосов', function() {
    return this.browser
      .openComponent('election', 'без данных по явке и числа обработанных голосов', 'desktop')
      .setViewportSize({ width: 648, height: 600 })
      .assertView('no-turnout-and-no-processed-ballots', '.news-election');
  });
});
