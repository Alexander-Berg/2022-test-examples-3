describe('Кнопка "читать далее"', () => {
  it('имеет внешний вид', function() {
    return this.browser
      .openComponent('readmorebutton', 'default', 'phone')
      .assertView('plain', '.mg-read-more-button');
  });
});
