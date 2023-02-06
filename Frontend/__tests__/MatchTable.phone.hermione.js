describe('Турнирная таблица', () => {
  it('должен корректно отображать матч без групп', function() {
    return this.browser
      .openComponent('MatchTable', 'Футбол', 'phone')
      .waitForVisible('.sport-match-table')
      .assertView('plain', '.sport-match-table');
  });

  it('должен корректно отображать матч c группами', function() {
    return this.browser
      .openComponent('MatchTable', 'Несколько групп', 'phone')
      .waitForVisible('.sport-match-table')
      .assertView('plain', '.sport-match-table');
  });
});
