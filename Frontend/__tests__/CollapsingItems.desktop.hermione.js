const height = 700;

hermione.only.notIn(['win-ie11'], 'статика для IE локализована, прогон тестов неактуален');
describe('Сворачивающиеся элементы', () => {
  it('должны отображаться корректно (default)', async function() {
    const widths = [1920, 1000, 600, 300];

    for (const width of widths) {
      await this.browser
        .setViewportSize({ width, height })
        .openComponent('collapsingitems', 'default')
        .yaAssertOuterView(`collapsing-default-${width}`, '.mg-collapsing-items', {
          paddings: 5,
          screenshotDelay: 100,
        });
    }
  });

  it('элементы должны правильно сворачиваться (default)', async function() {
    const widths = [1000, 600];

    for (const width of widths) {
      await this.browser
        .setViewportSize({ width, height })
        .openComponent('collapsingitems', 'default')
        .yaWaitForVisible('.mg-collapsing-items__item-more')
        .moveToObject('.mg-collapsing-items__item-more button', 10, 10)
        .yaWaitForVisible('.mg-collapsing-items__popup', 5000)
        .assertView(`collapsing-popup-default-${width}`, '.mg-collapsing-items__popup', {
          allowViewportOverflow: true,
          screenshotDelay: 500,
        });
    }
  });
});
