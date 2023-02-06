hermione.only.notIn(['win-ie11'], 'сторибук не открывается в ie');
describe('Плей-офф матчи', () => {
  it('Общий вид', function() {
    return this.browser
      .openComponent('PlayoffMatchList', 'Общий вид', 'desktop')
      .waitForVisible('.sport-playoff-match-list')
      .moveToObject('.sport-playoff-match:first-child')
      .assertView('plain', '.sport-playoff-match-list');
  });
});
