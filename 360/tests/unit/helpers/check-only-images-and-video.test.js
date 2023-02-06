const checkOnlyImagesAndVideo = require('../../../helpers/check-only-images-and-video');
const { PHOTO_GRID_COVER_THRESHOLD } = require('@ps-int/ufo-rocks/lib/consts');

const getImage = (width, height) => ({
    meta: {
        mediatype: 'image'
    },
    width: width || 400,
    height: height || 300
});

const getVideo = (width, height) => ({
    meta: {
        mediatype: 'video'
    },
    width: width || 400,
    height: height || 300
});

describe('checkOnlyImagesAndVideo', () => {
    it('должен вернуть `false` если папка пуста', () => {
        expect(checkOnlyImagesAndVideo([{
            id: 'root',
            type: 'dir'
        }])).toBe(false);
    });

    it('должен вернуть `true` если все ресурсы - фото', () => {
        expect(checkOnlyImagesAndVideo([
            {
                id: 'root',
                type: 'dir'
            }, getImage(), getImage()
        ])).toBe(true);
    });

    it('должен вернуть `true` если все ресурсы - видео', () => {
        expect(checkOnlyImagesAndVideo([
            {
                id: 'root',
                type: 'dir'
            }, getVideo(), getVideo()
        ])).toBe(true);
    });

    it('должен вернуть `true` если все ресурсы - фото или видео', () => {
        expect(checkOnlyImagesAndVideo([
            {
                id: 'root',
                type: 'dir'
            }, getImage(), getVideo(), getImage()
        ])).toBe(true);
    });

    it('должен вернуть `false` если есть подпапка', () => {
        expect(checkOnlyImagesAndVideo(
            [{
                id: 'root',
                type: 'dir'
            }, {
                id: 'subfolder',
                type: 'dir'
            }, getImage(), getVideo(), getImage()]
        )).toBe(false);
    });

    it('должен вернуть `false` если есть документ', () => {
        expect(checkOnlyImagesAndVideo(
            [{
                id: 'root',
                type: 'dir'
            }, getImage(), getVideo(), getImage(), {
                id: 'doc',
                meta: {
                    mediatype: 'document'
                },
            }]
        )).toBe(false);
    });

    it('должен вернуть `true` если несколько фоток - мелкие', () => {
        expect(checkOnlyImagesAndVideo(
            [
                {
                    id: 'root',
                    type: 'dir'
                },
                getImage(),
                getVideo(),
                getImage(PHOTO_GRID_COVER_THRESHOLD - 1),
                getImage(),
                getImage(PHOTO_GRID_COVER_THRESHOLD - 10)
            ]
        )).toBe(true);
    });

    it('должен вернуть `false` если большинство фоток - мелкие', () => {
        expect(checkOnlyImagesAndVideo(
            [
                {
                    id: 'root',
                    type: 'dir'
                },
                getImage(),
                getVideo(),
                getImage(PHOTO_GRID_COVER_THRESHOLD - 1),
                getImage(400, PHOTO_GRID_COVER_THRESHOLD - 10),
                getImage(500, PHOTO_GRID_COVER_THRESHOLD - 20)
            ]
        )).toBe(false);
    });

    it('должен вернуть `true` если у фоток нет размеров', () => {
        expect(checkOnlyImagesAndVideo(
            [
                {
                    id: 'root',
                    type: 'dir'
                },
                {
                    meta: {
                        mediatype: 'image'
                    }
                },
                {
                    meta: {
                        mediatype: 'image'
                    }
                }
            ]
        )).toBe(false);
    });
});
