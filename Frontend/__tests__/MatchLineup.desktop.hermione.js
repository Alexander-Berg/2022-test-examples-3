hermione.only.notIn(['linux-firefox', 'win-ie11'], 'сторибук не открывается в ie, проблемы со скроллом в firefox');

describe('Состав команд', () => {
  it('должен корректно отображать футбольный матч', function() {
    return this.browser
      .openComponent('MatchLineup', 'Футбол', 'desktop')
      .waitForVisible('.sport-match-lineup')
      .assertView('plain', '.sport-match-lineup');
  });
});
