describe('Сниппет официального комментария', () => {
  it('должен отображаться корректно', function() {
    return this.browser
      .openComponent('officialsnippet', 'default', 'phone')
      .assertView('plain', '.mg-official-snippet');
  });
});
