describe('Состав команд', () => {
  it('должен корректно отображать футбольный матч', function() {
    return this.browser
      .openComponent('MatchLineup', 'Футбол', 'phone')
      .waitForVisible('.sport-match-lineup')
      .assertView('plain', '.sport-match-lineup');
  });
});
