hermione.only.notIn(['linux-firefox', 'win-ie11'], 'сторибук не открывается в ie, проблемы со скроллом в firefox');
describe('Ход матча', () => {
  [
    'Хоккей',
    'Футбол',
    'Без событий',
  ].forEach((storyText) => {
    it(storyText, function() {
      return this.browser
        .openComponent('MatchChronology', storyText, 'desktop')
        .waitForVisible('.sport-match-chronology')
        .assertView('plain', '.sport-match-chronology');
    });
  });
});
