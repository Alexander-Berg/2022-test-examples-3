describe('Storybook', function() {
    describe('ThumbnailsGallery', function() {
        beforeEach(function() {
            return this.browser.yaOpenComponent('tests-thumbnailsgallery--plain', true);
        });

        it('default', async function() {
            return this.browser.yaAssertViewThemeStorybook('default', '.ThumbnailsGallery');
        });

        it('half-scrolled', async function() {
            await this.browser.yaScrollContainerToElem('.ThumbnailsGallery .NativeScroll-Inner', '.ThumbnailsGallery .NativeScroll-Inner .ThumbnailsGallery-Item:nth-child(2)');

            await this.browser.yaAssertViewThemeStorybook('half-scrolled', '.ThumbnailsGallery');
        });

        it('full-scrolled', async function() {
            await this.browser.yaScrollContainerToElem('.ThumbnailsGallery .NativeScroll-Inner', '.ThumbnailsGallery .NativeScroll-Inner .ThumbnailsGallery-Item:last-child');

            await this.browser.yaAssertViewThemeStorybook('full-scrolled', '.ThumbnailsGallery');
        });
    });
});
