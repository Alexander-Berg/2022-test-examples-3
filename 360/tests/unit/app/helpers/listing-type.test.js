const { getListingType } = require('../../../../app/helpers/listing-type');

const imageResource = {
    type: 'file',
    meta: { mediatype: 'image' }
};

const videoResource = {
    type: 'file',
    meta: { mediatype: 'video' }
};

const archiveResource = {
    type: 'file',
    meta: { mediatype: 'compressed' }
};

const folderResource = {
    type: 'dir'
};

describe('getLisingType', () => {
    describe('15 фотографий и 2 архива', () => {
        const resources = [
            {},
            ...(new Array(15).fill(imageResource)),
            ...(new Array(2).fill(archiveResource))
        ];

        it('должны отображаться крупной плиткой на десктопах', () => {
            expect(getListingType(resources, false)).toBe('tile');
        });

        it('должны отображаться мелкой плиткой на смартфонах', () => {
            expect(getListingType(resources, true)).toBe('icons');
        });
    });

    describe('14 фотографий и видео', () => {
        const resources = [
            {},
            ...(new Array(7).fill(imageResource)),
            ...(new Array(7).fill(videoResource))
        ];

        it('должны отображаться крупной плиткой на десктопах', () => {
            expect(getListingType(resources, false)).toBe('tile');
        });

        it('должны отображаться мелкой плиткой на смартфонах', () => {
            expect(getListingType(resources, true)).toBe('icons');
        });
    });

    describe('14 фотографий и 2 архива', () => {
        const resources = [
            {},
            ...(new Array(14).fill(imageResource)),
            ...(new Array(2).fill(archiveResource))
        ];

        it('должны отображаться списком на десктопах', () => {
            expect(getListingType(resources, false)).toBe('list');
        });

        it('должны отображаться списком на смартфонах', () => {
            expect(getListingType(resources, true)).toBe('list');
        });
    });

    describe('20 папок, 18 фотографий и 2 архива', () => {
        const resources = [
            {},
            ...(new Array(20).fill(folderResource)),
            ...(new Array(18).fill(imageResource)),
            ...(new Array(2).fill(archiveResource))
        ];

        it('должны отображаться списком на десктопах', () => {
            expect(getListingType(resources, false)).toBe('list');
        });

        it('должны отображаться списком на смартфонах', () => {
            expect(getListingType(resources, true)).toBe('list');
        });
    });

    describe('21 папка и 19 фотографий', () => {
        const resources = [
            {},
            ...(new Array(21).fill(folderResource)),
            ...(new Array(19).fill(imageResource))
        ];

        it('должны отображаться списком на десктопах', () => {
            expect(getListingType(resources, false)).toBe('list');
        });

        it('должны отображаться списком на смартфонах', () => {
            expect(getListingType(resources, true)).toBe('list');
        });
    });

    describe('20 папок и 20 фотографий', () => {
        const resources = [
            {},
            ...(new Array(20).fill(folderResource)),
            ...(new Array(20).fill(imageResource))
        ];

        it('должны отображаться крупной плиткой на десктопах', () => {
            expect(getListingType(resources, false)).toBe('tile');
        });

        it('должны отображаться мелкой плиткой на смартфонах', () => {
            expect(getListingType(resources, true)).toBe('icons');
        });
    });

    describe('20 папок', () => {
        const resources = [
            {},
            ...(new Array(20).fill(folderResource))
        ];

        it('должны отображаться списком на десктопах', () => {
            expect(getListingType(resources, false)).toBe('list');
        });

        it('должны отображаться списком на смартфонах', () => {
            expect(getListingType(resources, true)).toBe('list');
        });
    });

    describe('20 папок и 10 фотографий', () => {
        const resources = [
            {},
            ...(new Array(20).fill(folderResource)),
            ...(new Array(10).fill(imageResource))
        ];

        it('должны отображаться списком на десктопах', () => {
            expect(getListingType(resources, false)).toBe('list');
        });

        it('должны отображаться списком на смартфонах', () => {
            expect(getListingType(resources, true)).toBe('list');
        });
    });

    describe('10 папок и 11 фотографий', () => {
        const resources = [
            {},
            ...(new Array(10).fill(folderResource)),
            ...(new Array(11).fill(imageResource))
        ];

        it('должны отображаться крупной плиткой на десктопах', () => {
            expect(getListingType(resources, false)).toBe('tile');
        });

        it('должны отображаться мелкой плиткой на смартфонах', () => {
            expect(getListingType(resources, true)).toBe('icons');
        });

        it('должны отображаться крупной плиткой на десктопах, несмотря на сохраненную настройку', () => {
            expect(getListingType(resources, false, false, false, {
                settings: { typeListingPublic: 'list' }
            })).toBe('tile');
        });
    });

    describe('папка на вау-паблике, где доступна вау-сетка', () => {
        const resources = [
            {},
            ...(new Array(5).fill(imageResource))
        ];

        it('должны отображаться вау-сеткой на десктопе, если нет сохраненной настройки', () => {
            expect(getListingType(resources, false, true)).toBe('wow');
        });

        it('должны отображаться списком на десктопе, если сохранена соответствующая настройка', () => {
            expect(getListingType(resources, false, true, {
                settings: { typeListingWowPublic: 'list' }
            })).toBe('list');
        });

        it('должны отображаться мелкой плиткой на смартфонах, если в настроках - крупная плитка', () => {
            expect(getListingType(resources, true, true, {
                settings: { typeListingWowPublic: 'tile' }
            })).toBe('icons');
        });

        it('должны отображаться крупной плиткой на смартфонах, если в настроках - недопустимое значение', () => {
            expect(getListingType(resources, false, true, {
                settings: { typeListingWowPublic: 'tile123' }
            })).toBe('tile');
        });
    });

    describe('папка на вау-паблике, где недоступна вау-сетка', () => {
        const resources = [
            {},
            ...(new Array(2).fill(folderResource)),
            ...(new Array(5).fill(imageResource))
        ];

        it('должны отображаться списком на десктопе, если в настройках - список', () => {
            expect(getListingType(resources, false, false, {
                settings: { typeListingPublic: 'list' }
            })).toBe('list');
        });

        it('должны отображаться мелкой плиткой на смартфонах, если в настройках - крупная плитка', () => {
            expect(getListingType(resources, true, false, {
                settings: { typeListingPublic: 'tile' }
            })).toBe('icons');
        });

        it('должны отображаться крупной плиткой на десктопе, если в настройках - вау-сетка', () => {
            expect(getListingType(resources, false, false, {
                settings: { typeListingWowPublic: 'wow' }
            })).toBe('tile');
        });
    });
});
