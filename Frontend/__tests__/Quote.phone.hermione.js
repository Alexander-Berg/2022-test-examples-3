describe('Цитата', () => {
  it('должна отображаться корректно', function() {
    return this.browser
      .openComponent('quote', 'default')
      .assertView('quote', '.mg-quote');
  });
});
