const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { photoItemByName } = require('../page-objects/client');
const consts = require('../config').consts;
const albums = require('../page-objects/client-albums-page');
const photos = require('../page-objects/client-photo2-page');
const popups = require('../page-objects/client-popups').common;
const slider = require('../page-objects/slider').common;
const { assert } = require('chai');

describe('Альбомы 2 ->', () => {
    it('diskclient-5638, diskclient-5810: Сохраняется вид "обычный" в личном альбоме', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5810' : 'diskclient-5638';

        await bro.yaClientLoginFast('yndx-ufo-test-301');
        await bro.url('/client/albums/5e1c59102ba5f2211b126e1f');
        await bro.yaWaitAlbumItemsInViewportLoad();
        await bro.yaAssertView(this.testpalmId, albums.album2());
    });

    it('diskclient-5805, diskclient-5811: Сохраняется вид "Умная плитка" в личном альбоме', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5811' : 'diskclient-5805';

        await bro.yaClientLoginFast('yndx-ufo-test-301');
        await bro.url('/client/albums/5e1c57df3c9286c0faade73c');
        await bro.yaWaitAlbumItemsInViewportLoad();
        await bro.yaAssertView(this.testpalmId, albums.album2());
    });

    it('diskclient-5654, diskclient-5820: Отображение длинного названия на странице личного альбома', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5820' : 'diskclient-5654';

        await bro.yaClientLoginFast('yndx-ufo-test-301');
        await bro.url('/client/albums/5e31a18e6048a2cd7c5f63b8');
        await bro.yaWaitAlbumItemsInViewportLoad();
        await bro.yaAssertView(this.testpalmId, albums.album2());
    });

    it('diskclient-5992, diskclient-5993: Отображение названия личного альбома с символами Й и Ё', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5993' : 'diskclient-5992';

        await bro.yaClientLoginFast('yndx-ufo-test-422');
        await bro.url('/client/albums/5e4cfa0fd36535dfd1377887');
        await bro.yaWaitAlbumItemsInViewportLoad();
        await bro.yaAssertView(this.testpalmId, albums.album2());
    });

    it('diskclient-5651, diskclient-5815: Переход в личный альбом', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-301');
        await bro.url('/client/albums');
        await bro.yaWaitForVisible(albums.albums2.personal.album());
        await bro.click(albums.albums2.personal.album());
        await bro.yaWaitAlbumItemsInViewportLoad();
        await bro.yaWaitForVisible(albums.album2.itemByName().replace(':title', '19-44.jpg'));
    });

    it('diskclient-5994, diskclient-5995: Выход из личного альбома по стрелке назад', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-301');
        await bro.url('/client/albums/5e1c57df3c9286c0faade73c');
        await bro.yaWaitAlbumItemsInViewportLoad();

        await bro.click(albums.album2.backButton());
        await bro.yaWaitForHidden(albums.album2());
        await bro.yaWaitForVisible(albums.albums2.personal.album());

        const title = await bro.getText(albums.albums.title());
        assert.equal(title, 'Альбомы');
    });

    it('diskclient-5640, diskclient-5813: Подгрузка файлов в личном альбоме с типом сетки умная плитка', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-301');
        await bro.url('/client/albums/5e1c57df3c9286c0faade73c');
        await bro.yaWaitAlbumItemsInViewportLoad();
        await bro.yaScrollToEnd();
        const lastPhotoSelector = albums.album2.itemByName().replace(':title', '17-48.jpg');
        await bro.yaWaitForVisible(lastPhotoSelector);
    });

    hermione.skip.in('firefox-desktop', 'https://st.yandex-team.ru/CHEMODAN-76969');
    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-5645: Выделение областью мыши в личном альбоме', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-5645';

        await bro.yaClientLoginFast('yndx-ufo-test-301');
        await bro.url('/client/albums/5e1c57df3c9286c0faade73c');
        await bro.yaWaitAlbumItemsInViewportLoad();

        const { x, y } = await bro.getLocation(albums.album2.title());
        const { width, height } = await bro.getViewportSize();

        const options = {
            startX: x - 10,
            startY: y - 10,
            deltaX: width - x - 700,
            deltaY: height / 2,
            releaseMouse: false
        };
        await bro.yaMouseSelect(albums.album2.title(), options);

        await bro.yaAssertView(this.testpalmId, albums.album2());

        await bro.yaMouseSelect(albums.album2.title(), Object.assign(options, { releaseMouse: true }));

        assert.deepEqual(
            await bro.yaGetSelectedPhotoItemsNames(),
            ['Space to roam.mp4', 'SpaceX Crew Dragon.mp4', 'The Talking Tree.mp4', '19-44.jpg']
        );
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-5644: Выделение с шифтом в личном альбоме', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-301');
        await bro.url('/client/albums/5e1c59102ba5f2211b126e1f');
        await bro.yaWaitAlbumItemsInViewportLoad();

        await bro.yaSelectPhotoItem(photoItemByName().replace(':title', '19-44.jpg'), true);

        await bro.yaClickWithPressedKey(photoItemByName().replace(':title', '12-48.jpg'), consts.KEY_SHIFT);

        assert.deepEqual(
            await bro.yaGetSelectedPhotoItemsNames(),
            ['19-44.jpg', '18-46.jpg', '12-48.jpg']
        );
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-5949: Отображение КМ для файла в личном альбоме', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-5949';
        await bro.yaClientLoginFast('yndx-ufo-test-301');
        await bro.url('/client/albums/5e1c57df3c9286c0faade73c');
        await bro.yaWaitAlbumItemsInViewportLoad();

        await bro.rightClick(albums.album2.item() + ':nth-child(2)');
        await bro.yaWaitForVisible(popups.actionPopup());
        await bro.pause(200); // Анимация рамки фотографиии при выделении
        await bro.yaAssertView(this.testpalmId, albums.album2());
    });

    it('diskclient-5770, diskclient-5886: Ограничение при добавлении 501 файла в личный альбом', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5886' : 'diskclient-5770';

        await bro.yaClientLoginFast('yndx-ufo-test-301');

        await bro.url('/client/albums/5e1c57df3c9286c0faade73c');
        await bro.yaWaitForVisible(albums.album2.addToAlbumButton());
        await bro.click(albums.album2.addToAlbumButton());

        await bro.yaWaitPhotoSliceItemsInViewportLoad();
        await bro.yaWaitPhotosliceWithToolbarOpened();

        await bro.click(photos.common.photo.itemByName().replace(':title', '13-10.jpg')); // первое фото

        await bro.yaScrollToEnd();
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        // последнее фото
        await bro.yaClickWithPressedKey(photos.common.photo.itemByName().replace(':title', '9-25.jpg'),
            consts.KEY_SHIFT);

        await bro.yaWaitForVisible(popups.actionBar.selectionInfoSpin());
        await bro.yaWaitForHidden(popups.actionBar.selectionInfoSpin());

        await bro.yaWaitForVisible(popups.selectionInfoLimitTooltip());
        await bro.pause(200);

        await bro.yaAssertView(this.testpalmId, [
            photos.common.addToAlbumBar(), popups.selectionInfoLimitTooltip()
        ]);
    });

    it('diskclient-5724, diskclient-5847: Открыть слайдер в личном альбоме', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5847' : 'diskclient-5724';

        await bro.yaClientLoginFast('yndx-ufo-test-301');
        await bro.url('/client/albums/5e1c57df3c9286c0faade73c');

        const itemSelector = albums.album2.itemByName().replace(':title', '18-46.jpg');
        await bro.yaWaitForVisible(itemSelector);
        await bro.click(itemSelector);

        const previewImage = await bro.$(slider.contentSlider.previewImage());

        await previewImage.waitForExist({ timeout: 5000 });
        await bro.yaAssertView(this.testpalmId, slider.contentSlider());
    });

    it('diskclient-5726, 5169, 5849, 6194: Закрытие слайдера и подскролл при закрытии слайдера личного альбома', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-301');
        await bro.url('/client/albums/5e1c57df3c9286c0faade73c');

        const itemSelector = albums.album2.itemByName().replace(':title', '18-46.jpg');
        await bro.yaWaitForVisible(itemSelector);
        await bro.click(itemSelector);

        await bro.yaWaitForVisible(slider.contentSlider.items());
        await bro.yaChangeSliderActiveImage(50);

        assert.equal(await bro.yaGetActiveSliderImageName(), '12-18.jpg');

        await bro.click(slider.sliderButtons.closeButton());
        await bro.yaWaitForHidden(slider.contentSlider.previewImage());

        const targetItemSelector = albums.album2.itemByName().replace(':title', '12-18.jpg');
        await bro.yaWaitForVisible(targetItemSelector);
        await bro.yaAssertInViewport(targetItemSelector);
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-5725, diskclient-5845: Открыть слайдер в личном альбоме по прямой ссылке', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-301');
        // eslint-disable-next-line max-len
        await bro.url('/client/albums/5e1c57df3c9286c0faade73c?dialog=slider&idDialog=%2Fdisk%2FЗагрузки%2Fpopular3%2F12-18.jpg');

        await bro.yaWaitForVisible(slider.contentSlider.activePreview.image());
        assert.equal(await bro.yaGetActiveSliderImageName(), '12-18.jpg');

        await bro.yaWaitForVisible(slider.contentSlider.nextImage());
        await bro.click(slider.contentSlider.nextImage());

        assert.equal(await bro.yaGetActiveSliderImageName(), '17-25.jpg');
    });
});
