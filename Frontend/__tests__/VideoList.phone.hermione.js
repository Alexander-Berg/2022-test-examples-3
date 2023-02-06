describe('Видеокарусель', () => {
  it('должна корректно отображаться', function() {
    return this.browser
      .openComponent('VideoList', 'обычный вид', 'phone')
      .execute(() => {
        const stubImage = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="100%25" height="100%25"%3E%3Cstyle%3Eline%7Bstroke:%23ccc%7D%3C/style%3E%3Cdefs%3E%3Cpattern id="grid" patternUnits="userSpaceOnUse" width="10" height="10" patternTransform="rotate(45)"%3E%3Cline x1="5" y1="0" x2="5" y2="10"/%3E%3Cline x1="0" y1="5" x2="10" y2="5"/%3E%3C/pattern%3E%3C/defs%3E%3Crect width="100%25" height="100%25" fill="url(%23grid)"/%3E%3C/svg%3E%0A';
        const previews = document.querySelectorAll('.sport-video-list__item-preview');

        previews.forEach((item) => {
          item.style.backgroundImage = `url('${stubImage}')`;
        });
      })
      .assertView('plain', '#container');
  });
});
