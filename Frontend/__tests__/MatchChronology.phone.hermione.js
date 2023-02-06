describe('Ход матча', () => {
  [
    'Хоккей',
    'Футбол',
    'Без событий',
  ].forEach((storyText) => {
    it(storyText, function() {
      return this.browser
        .openComponent('MatchChronology', storyText, 'phone')
        .waitForVisible('.sport-match-chronology')
        .assertView('plain', '.sport-match-chronology');
    });
  });
});
