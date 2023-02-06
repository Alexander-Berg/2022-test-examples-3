describe('edu-components_Toaster', () => {
  hermione.skip.in(/./, 'https://st.yandex-team.ru/ISL-9518');
  it('should properly render', function() {
    return this.browser
      .setViewportSize({
        width: 500,
        height: 500,
      })
      .openComponent('edu-components', 'toaster', 'default')
      .assertView('plain', ['.Gemini'], { allowViewportOverflow: true });
  });
});
