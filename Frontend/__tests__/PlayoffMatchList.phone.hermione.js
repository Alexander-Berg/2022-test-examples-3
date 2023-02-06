describe('Плей-офф матчи', () => {
  it('Общий вид', function() {
    return this.browser
      .openComponent('PlayoffMatchList', 'Общий вид')
      .waitForVisible('.sport-playoff-match-list')
      .assertView('plain', '.sport-playoff-match-list');
  });
});
