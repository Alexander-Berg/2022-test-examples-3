hermione.only.notIn(['win-ie11'], 'сторибук не открывается в ie');
describe('Список турниров', () => {
  it('отображается корректно', function() {
    return this.browser
      .openComponent('TournamentTeaser', 'Общий вид', 'desktop')
      .setViewportSize({ width: 916, height: 480 })
      .assertView('plain', '.sport-tournament-teaser__storybook-wrapper');
  });
});
