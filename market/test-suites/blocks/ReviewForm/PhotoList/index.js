import {makeCase, makeSuite, mergeSuites} from 'ginny';

import PhotoList from '@self/platform/components/ReviewForm/PhotoList/__pageObject';
import Notification from '@self/root/src/components/Notification/__pageObject';

const MAX_UPLOADING_PHOTO_COUNT = 15;

export default makeSuite('Прикрепление фотографий к отзыву.', {
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                photoList: () => this.createPageObject(PhotoList),
                notification: () => this.createPageObject(Notification),
            });
        },
        'По умолчанию': {
            'загружается до 15 фотографий': makeCase({
                id: 'm-touch-3326',
                async test() {
                    await this.browser.allure.runStep(
                        'Добавляем к отзыву 15 фотографий',
                        () => uploadNImages.call(this, MAX_UPLOADING_PHOTO_COUNT));
                    await this.photoList.waitForLoadedPhotosCountChange(MAX_UPLOADING_PHOTO_COUNT);
                },
            }),
            'невозможно загрузить более 15 фотографий': makeCase({
                id: 'm-touch-3327',
                async test() {
                    await this.browser.allure.runStep(
                        'Добавляем к отзыву 15 фотографий',
                        () => uploadNImages.call(this, 15));

                    await this.photoList.waitForLoadedPhotosCountChange(MAX_UPLOADING_PHOTO_COUNT);

                    // превью появляются почти сразу, но загрузка фоток происходит не сразу
                    // и нужно подождать -- как сделать это через ожидание непонятно

                    // eslint-disable-next-line market/ginny/no-pause
                    await this.browser.pause(1000);

                    await this.browser.allure.runStep(
                        'Загружаем еще лишние фото',
                        () => uploadNImages.call(this, 5));

                    await this.notification.waitForText('Можно загрузить только 15 фотографий');

                    return this.photoList.getUploadedPhotosCount()
                        .should.eventually.equal(MAX_UPLOADING_PHOTO_COUNT, 'Количество фото не изменилось');
                },
            }),
        },
    }),
});

/**
 * Рекурсивно прикрепляет к отзыву n-ое количество фотографий
 *
 * @param {Number} nPics количество фотографий, которое хотим загрузить
 * @return {Promise}
 */
function uploadNImages(nPics) {
    if (nPics === 0) {
        return Promise.resolve(nPics);
    }

    return new Promise(resolve => {
        this.photoList.choosePhoto()
            .then(() => uploadNImages.call(this, nPics - 1))
            .then(() => resolve(nPics));
    });
}
