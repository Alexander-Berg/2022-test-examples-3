hermione.only.notIn(['linux-firefox', 'win-ie11'], 'сторибук не открывается в ie, проблемы со скроллом в firefox');
describe('Матч-центр', () => {
  it('должен корректно отображаться', function() {
    return this.browser
      .openComponent('MatchCalendar', 'Матч-центр')
      .waitForVisible('.sport-match-calendar')
      .assertView('plain', '.sport-match-calendar')
      .click('.mg-navigation-menu__item-wrap:nth-child(3)')
      .assertView('basketball', '.sport-match-calendar')
      .click('.sport-match-competition__open')
      .assertView('opened', '.sport-match-calendar');
  });
});
