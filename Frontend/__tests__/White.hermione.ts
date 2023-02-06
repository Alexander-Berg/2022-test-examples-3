// @ts-nocheck
describe('FavoritesIcon', () => {
  it('should render white icon', function() {
    return this.browser
      .openScenario('FavoritesIcon', 'White')
      .assertView('white', ['.FavoritesIcon']);
  });
});
