describe('Кнопка "читать далее"', () => {
  hermione.only.notIn('win-ie11');
  it('имеет внешний вид', function() {
    return this.browser
      .openComponent('readmorebutton', 'default', 'desktop')
      .assertView('plain', '.mg-read-more-button')
      .moveToObject('.mg-read-more-button')
      .assertView('hovered', '.mg-read-more-button');
  });
});
