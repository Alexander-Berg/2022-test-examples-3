// @ts-nocheck
describe('FavoritesIcon', () => {
  it('should render icon', function() {
    return this.browser
      .openScenario('FavoritesIcon', 'Simple')
      .assertView('simple', ['.FavoritesIcon'])
      .moveToObject('.FavoritesIcon')
      .assertView('hovered', ['.FavoritesIcon']);
  });
});
