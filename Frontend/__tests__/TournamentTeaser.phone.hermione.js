describe('Список турниров', () => {
  it('отображается корректно', function() {
    return this.browser
      .openComponent('TournamentTeaser', 'Общий вид', 'phone')
      .assertView('plain', '.sport-tournament-teaser__storybook-wrapper');
  });
});
