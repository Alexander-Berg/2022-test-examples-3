hermione.only.notIn(['linux-firefox', 'win-ie11'], 'сторибук не открывается в ie, проблемы со скроллом в firefox');

describe('Турнирная таблица', () => {
  it('должен корректно отображать матч без групп', function() {
    return this.browser
      .openComponent('MatchTable', 'Футбол', 'desktop')
      .waitForVisible('.sport-match-table')
      .assertView('plain', '.sport-match-table');
  });

  it('должен корректно отображать матч c группами', function() {
    return this.browser
      .openComponent('MatchTable', 'Несколько групп', 'desktop')
      .waitForVisible('.sport-match-table')
      .assertView('plain', '.sport-match-table');
  });
});
