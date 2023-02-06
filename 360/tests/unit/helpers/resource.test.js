import '../noscript';
import _ from 'lodash';
import resourceHelper, { NAME_VALIDATION_ERRORS } from '../../../components/helpers/resource';
import getStore from '../../../components/redux/store';
import { createResource } from '../../../components/redux/store/actions/resources';

describe('resourceHelper', () => {
    describe('preprocess', () => {
        const tests = [{
            args: [{
                id: '/disk/path/to/folder/file.jpg',
                type: 'file',
                meta: {}
            }],
            benchmark: {
                id: '/disk/path/to/folder/file.jpg',
                type: 'file',
                meta: {},
                state: {},
                name: 'file.jpg',
                isInTrash: false,
                isEditable: false,
                parents: [{
                    id: '/disk',
                    name: 'Файлы'
                }, {
                    id: '/disk/path',
                    name: 'path'
                }, {
                    id: '/disk/path/to',
                    name: 'to'
                }, {
                    id: '/disk/path/to/folder',
                    name: 'folder'
                }],
                icon: 'image',
                hasPreview: false,
                app: undefined,
                isAvailableToAlbum: false,
                ext: 'jpg',
                canPlayVideo: false

            }
        }, {
            args: [{
                id: '/trash/path///file.jpg',
                type: 'file',
                name: 'file.jpg',
                meta: {
                    mediatype: 'image',
                    mimetype: 'image/jpeg',
                    sizes: {}
                }
            }],
            benchmark: {
                id: '/trash/path/file.jpg',
                type: 'file',
                meta: {
                    mediatype: 'image',
                    mimetype: 'image/jpeg',
                    sizes: {},
                    hasPreview: true
                },
                state: {},
                name: 'file.jpg',
                isInTrash: true,
                isEditable: false,
                parents: [{
                    id: '/trash',
                    name: 'Корзина'
                }, {
                    id: '/trash/path',
                    name: 'path'
                }],
                icon: 'image',
                hasPreview: true,
                app: null,
                isAvailableToAlbum: false,
                ext: 'jpg',
                canPlayVideo: false
            }
        }, {
            args: [{
                id: '/disk/pa&th///my_dir',
                name: 'Моя папка',
                type: 'dir'
            }],
            benchmark: {
                id: '/disk/pa&th/my_dir',
                name: 'Моя папка',
                type: 'dir',
                meta: {},
                state: {},
                isInTrash: false,
                isEditable: false,
                parents: [{
                    id: '/disk',
                    name: 'Файлы'
                }, {
                    id: '/disk/pa&th',
                    name: 'pa&th'
                }],
                icon: 'dir',
                hasPreview: false,
                isAvailableToAlbum: false,
                ext: '',
                canPlayVideo: false,
                iconType: 'dir'
            }
        }, {
            args: [{
                id: '/disk/audio.mp3',
                name: 'audio.mp3',
                type: 'file',
                meta: {
                    mimetype: 'audio/mpeg'
                }
            }],
            benchmark: {
                id: '/disk/audio.mp3',
                name: 'audio.mp3',
                type: 'file',
                meta: {
                    mimetype: 'audio/mpeg'
                },
                state: {},
                isInTrash: false,
                isEditable: false,
                parents: [{
                    id: '/disk',
                    name: 'Файлы'
                }],
                icon: 'audio',
                hasPreview: false,
                app: undefined,
                isAvailableToAlbum: false,
                ext: 'mp3',
                canPlayVideo: false
            }
        }, {
            args: [{
                id: '/disk/AUDIO.MP3',
                name: 'AUDIO.MP3',
                type: 'file',
                meta: {}
            }],
            benchmark: {
                id: '/disk/AUDIO.MP3',
                name: 'AUDIO.MP3',
                type: 'file',
                meta: {},
                state: {},
                isInTrash: false,
                isEditable: false,
                parents: [{
                    id: '/disk',
                    name: 'Файлы'
                }],
                icon: 'audio',
                hasPreview: false,
                app: undefined,
                isAvailableToAlbum: false,
                ext: 'mp3',
                canPlayVideo: false
            }
        }, {
            args: [{
                id: '/disk/Img.jpeg',
                name: 'Img.jpeg',
                type: 'file',
                meta: {},
                state: { part: true }
            }],
            benchmark: {
                id: '/disk/Img.jpeg',
                name: 'Img.jpeg',
                type: 'file',
                meta: {},
                state: { part: true },
                isInTrash: false,
                parents: [{ id: '/disk', name: 'Файлы' }],
                icon: 'image',
                app: null,
                hasPreview: false,
                isEditable: false,
                isAvailableToAlbum: false,
                ext: 'jpeg',
                canPlayVideo: false
            }
        }, {
            args: [{
                id: '/attach/yaruarchive/Архив моих записей test.zip_1406911780.36',
                name: 'Архив моих записей test.zip',
                type: 'file'
            }],
            benchmark: {
                id: '/attach/yaruarchive/Архив моих записей test.zip_1406911780.36',
                name: 'Архив моих записей test.zip',
                type: 'file',
                meta: {},
                state: {},
                isInTrash: false,
                parents: [{ id: '/disk', name: 'Файлы' }, { id: '/attach/yaruarchive', name: 'Другие сервисы' }],
                icon: 'zip',
                app: 'docviewer',
                canPlayVideo: false,
                hasPreview: false,
                isEditable: false,
                isAvailableToAlbum: false,
                ext: 'zip'
            }
        }];

        _.each(tests, (test) => {
            it('should return resource named as "' + test.benchmark.name + '" ', function() {
                const result = resourceHelper.preprocess.apply(this, test.args);

                expect(result).toEqual(test.benchmark);
            });
        });

        describe('При налиии meta.group', () => {
            it('Должен выставить флаг readonly для 640', () => {
                expect(
                    resourceHelper.preprocess({
                        meta: { group: { rights: 640, gid: 1 } }
                    }).meta.group.readonly
                ).toBe(true);
            });

            it('Должен выставить флаг in_readonly для 640', () => {
                expect(
                    resourceHelper.preprocess({
                        meta: { group: { rights: 640, is_root: false, gid: 1 } }
                    }).meta.group.in_readonly
                ).toBe(true);
            });

            it('Не должен выставить флаг in_readonly для общей папки', () => {
                expect(
                    resourceHelper.preprocess({
                        meta: { group: { rights: 640, is_root: true, gid: 1 } }
                    }).meta.group.in_readonly
                ).not.toBe(true);
            });
        });
    });

    describe('getParents', () => {
        const tests = [{
            args: ['disk//path//to////folder///file.jpg///'],
            benchmark: [{
                id: '/disk',
                name: 'Файлы'
            }, {
                id: '/disk/path',
                name: 'path'
            }, {
                id: '/disk/path/to',
                name: 'to'
            }, {
                id: '/disk/path/to/folder',
                name: 'folder'
            }]
        }, {
            args: ['/'],
            benchmark: []
        }, {
            args: ['/disk'],
            benchmark: []
        }];

        _.each(tests, (test) => {
            it('shold return parents with length "' + test.benchmark.length + '" ', function() {
                const result = resourceHelper.getParents.apply(this, test.args);
                expect(result).toEqual(test.benchmark);
            });
        });
    });

    describe('getAlias', () => {
        it('Должен вернуть Диск', () => {
            const resource = getStore().dispatch(createResource({
                id: '/disk',
                name: resourceHelper.DEFAULT_FOLDERS_DATA['/disk'].name
            }));
            const result = resourceHelper.getAlias(resource.id);
            expect(result).toEqual(resource.name);
        });
    });

    describe('Метод `preserveData`', () => {
        it('Должен сохранить нужные данные', () => {
            expect(resourceHelper.preserveData({
                id: '/disk/1',
                clusterId: 'c1',
                meta: {
                    mediatype: 'video'
                },
                type: 'file'
            }, {
                id: '/disk/1',
                state: {
                    part: true,
                    blocked: true
                },
                meta: {
                    mediatype: 'video',
                    size: 100,
                    videoInfo: {
                        streamId: '123',
                        videos: [{
                            dimension: '240p',
                            size: { width: 136, height: 240 },
                            height: 240,
                            width: 136,
                            url: 'url'
                        }]
                    }
                },
                originalSize: 150
            })).toEqual({
                id: '/disk/1',
                clusterId: 'c1',
                state: {
                    blocked: true
                },
                meta: {
                    mediatype: 'video',
                    size: 100,
                    videoInfo: {
                        streamId: '123',
                        videos: [{
                            dimension: '240p',
                            size: { width: 136, height: 240 },
                            height: 240,
                            width: 136,
                            url: 'url'
                        }]
                    }
                },
                type: 'file',
                originalSize: 150
            });
        });
    });

    describe('Метод `castSuggestItemToResource`', () => {
        it('должен создать папку', () => {
            expect(resourceHelper.castSuggestItemToResource({
                id: '123abc',
                key: '/disk/fldr',
                text: 'fldr',
                type: 'folders'
            })).toMatchObject({
                id: '/disk/fldr',
                name: 'fldr',
                type: 'dir',
                path: '/disk/fldr',
                meta: {
                    resource_id: '123abc'
                }
            });
        });
        it('должен создать файл', () => {
            expect(resourceHelper.castSuggestItemToResource({
                id: '123abc',
                key: '/disk/somefile.txt',
                text: 'somefile.txt',
                type: 'files'
            })).toMatchObject({
                id: '/disk/somefile.txt',
                name: 'somefile.txt',
                type: 'file',
                path: '/disk/somefile.txt',
                meta: {
                    resource_id: '123abc'
                }
            });
            expect(resourceHelper.castSuggestItemToResource({
                id: '123abc',
                key: '/disk/somefile.png',
                text: 'somefile.png',
                type: 'files',
                mimetype: 'image/png',
                mediatype: 'image'
            })).toMatchObject({
                id: '/disk/somefile.png',
                name: 'somefile.png',
                type: 'file',
                path: '/disk/somefile.png',
                meta: {
                    mimetype: 'image/png',
                    mediatype: 'image',
                    resource_id: '123abc',
                    sizes: []
                }
            });
            expect(resourceHelper.castSuggestItemToResource({
                id: '123abc',
                key: '/disk/somefile.mp4',
                text: 'somefile.mp4',
                type: 'files',
                mimetype: 'video/mp4',
                mediatype: 'video'
            })).toMatchObject({
                id: '/disk/somefile.mp4',
                name: 'somefile.mp4',
                type: 'file',
                path: '/disk/somefile.mp4',
                meta: {
                    mimetype: 'video/mp4',
                    mediatype: 'video',
                    resource_id: '123abc',
                    sizes: []
                }
            });
        });
    });

    describe('Метод `isOpenableImage`', () => {
        it('должен вернуть `false` для ресурса без меты', () => {
            expect(resourceHelper.isOpenableImage({})).toBe(false);
        });
        it('должен вернуть `false` для ресурса без медиатипа', () => {
            expect(resourceHelper.isOpenableImage({ meta: {} })).toBe(false);
        });
        it('должен вернуть `false` для ресурса без размеров', () => {
            expect(resourceHelper.isOpenableImage({ meta: { mediatype: 'image' } })).toBe(false);
        });
        it('должен вернуть `false` для ресурса с медиатипом, отличным от `image`', () => {
            expect(resourceHelper.isOpenableImage({ meta: { mediatype: 'video' } })).toBe(false);
        });
        it('должен вернуть `true` для ресурса с медиатипом `image` и размерами', () => {
            expect(resourceHelper.isOpenableImage({ meta: { mediatype: 'image', sizes: [] } })).toBe(true);
        });
    });

    describe('Метод `isResourceSharedRoot`', () => {
        it('должен вернуть `false` для ресурса без меты', () => {
            expect(resourceHelper.isResourceSharedRoot({})).toBe(false);
        });
        it('должен вернуть `false` для ресурса без группы', () => {
            expect(resourceHelper.isResourceSharedRoot({ meta: {} })).toBe(false);
        });
        it('должен вернуть `false` для ресурса без поля `is_shared`', () => {
            expect(resourceHelper.isResourceSharedRoot({ meta: { group: {} } })).toBe(false);
        });
        it('должен вернуть `false` для ресурса без поля `is_root`', () => {
            expect(resourceHelper.isResourceSharedRoot({ meta: { group: {} } })).toBe(false);
        });
        it('должен вернуть `true` для пошаренной папки', () => {
            expect(resourceHelper.isResourceSharedRoot({ meta: { group: { is_shared: true, is_root: true } } })).toBe(true);
        });
    });

    describe('Метод `validateName`', () => {
        it('Не ругается на нормальные имена', () => {
            expect(resourceHelper.validateName('')).toBeFalsy();
            expect(resourceHelper.validateName('foo')).toBeFalsy();
            expect(resourceHelper.validateName('имя файла???')).toBeFalsy();
        });

        it('Не ругается на строку длиной 255', () => {
            expect(resourceHelper.validateName('x'.repeat(255))).toBeFalsy();
        });

        it('Возвращает `NAME_EXCEEDS_MAX_LENGTH` для строки длиной 256', () => {
            expect(resourceHelper.validateName('x'.repeat(256))).toBe(NAME_VALIDATION_ERRORS.NAME_EXCEEDS_MAX_LENGTH);
        });

        it('Возвращает `NAME_HAS_SLASH` для строки с "/"', () => {
            expect(resourceHelper.validateName('/')).toBe(NAME_VALIDATION_ERRORS.NAME_HAS_SLASH);
        });
    });
});
