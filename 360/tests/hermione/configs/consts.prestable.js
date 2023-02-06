const content = require('../page-objects/client-content-listing').common;
const tuning = require('../page-objects/client-tuning-page').common;
const { photo, photoHeader } = require('../page-objects/client-photo2-page').common;
const { albums2 } = require('../page-objects/client-albums-page');

const selectors = {
    content: content.listing(),
    header: content.listing.head.header()
};
/**
 * Функция возвращающая шаблон текста в нотифайке о действии,
 * происходящем над несколькими файлами.
 *
 * @param {string} action - совершаемое действие
 * @param {string|number} count - количество файлов
 * @returns {string}
 */
const templateForManyFiles = (action, count) => {
    const lastNumber = Number(count.toString().slice(-1));
    let end = '';
    if (lastNumber === 0 || lastNumber > 4) {
        end = 'ов';
    } else if (lastNumber > 1) {
        end = 'а';
    }
    return `${action} ${count} объект${end} в папку «:name»`;
};

const CONSTS = {
    PASSPORT_URL: 'https://passport.yandex.ru',
    CLIENT_SET_COOKIE_URL: '/version/',
    CLIENT: '/client/disk',
    CLIENT_ROOT_URL: '/',

    NAVIGATION: {
        recent: {
            url: '/client/recent',
            name: 'Recent',
            navTitle: 'Последние',
            contentTitle: 'Последние файлы',
            selectors
        },
        disk: {
            url: '/client/disk',
            name: 'Disk',
            navTitle: 'Файлы',
            contentTitle: 'Файлы',
            selectors
        },
        downloads: {
            url: '/client/disk/Загрузки',
            name: 'Downloads',
            navTitle: 'Загрузки',
            contentTitle: 'Загрузки',
            selectors
        },
        photo: {
            url: '/client/photo',
            searchAppUrl: '/search-app',
            name: 'Photo',
            navTitle: 'Фото',
            contentTitle: 'Фото',
            selectors: {
                content: photo(),
                header: photoHeader.title()
            }
        },
        albums: {
            url: '/client/albums',
            navTitle: 'Альбомы',
            contentTitle: 'Альбомы',
            name: 'Albums',
            selectors: {
                content: albums2(),
                header: albums2.header()
            }
        },
        geoAlbums: {
            url: '/client/albums/geo',
            navTitle: 'Места и поездки',
            contentTitle: 'Места и поездки',
            name: 'GeoAlbums',
            selectors
        },
        scans: {
            url: '/client/disk/Сканы',
            name: 'Scans',
            navTitle: 'Сканы',
            contentTitle: 'Сканы',
            selectors
        },
        shared: {
            url: '/client/shared',
            name: 'Shared',
            navTitle: 'Общий доступ',
            contentTitle: 'Общий доступ',
            selectors
        },
        published: {
            url: '/client/published',
            name: 'Published',
            navTitle: 'Общий доступ',
            contentTitle: 'Общий доступ',
            selectors
        },
        trash: {
            url: '/client/trash',
            name: 'Trash',
            navTitle: 'Корзина',
            contentTitle: 'Корзина',
            selectors
        },
        archive: {
            url: '/client/attach',
            name: 'Archive',
            navTitle: 'Архив',
            contentTitle: 'Архив',
            selectors
        },
        journal: {
            url: '/client/journal',
            name: 'Journal',
            navTitle: 'История',
            contentTitle: 'История',
            selectors: {
                content: content.journalListing(),
                header: content.journalListing.header()
            }
        },
        folder: (path) => {
            const isArray = Array.isArray(path);
            const folderName = isArray ? path[path.length - 1] : path;
            return {
                url: `/client/disk/${isArray ? path.join('/') : folderName}`,
                name: folderName,
                navTitle: folderName,
                contentTitle: folderName,
                selectors
            };
        },
        tuning: {
            url: '/tuning',
            name: 'Tuning',
            navTitleTouch: 'купить место',
            contentTitle: 'Купите подписку Диск Про',
            selectors: {
                content: tuning.tuningPage(),
                header: tuning.tuningPage.title()
            }
        }
    },

    LISTING: {
        tile: 'tile',
        icons: 'icons',
        list: 'list'
    },

    TEST_FOLDER_NAME: 'test-folder',
    // eslint-disable-next-line max-len
    TEST_255_CHAR_NAME: 'veryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryverylongnam',

    FILE_OPERATIONS_TIMEOUT: 25000,
    ASSERT_TIMEOUT: 5000,

    TEXT_NOTIFICATION_OBJECTS_MOVED_TO_FOLDER: (count) => {
        return templateForManyFiles('Перемещено', count);
    },
    TEXT_NOTIFICATION_OBJECTS_COPIED_TO_FOLDER: (count) => {
        return templateForManyFiles('Скопировано', count);
    },
    TEXT_NOTIFICATION_FOLDER_CREATED: 'Вы создали папку :name',
    TEXT_NOTIFICATION_FOLDER_COPIED: 'Папка «:name» скопирована в папку «Файлы»',
    TEXT_NOTIFICATION_FOLDER_UNPUBLISH: 'Публичная ссылка на папку «:name» удалена',
    // eslint-disable-next-line max-len
    TEXT_NOTIFICATION_FOLDER_PUBLISH_NO_SPACE: 'Не удалось опубликовать папку «:name». На вашем Диске закончилось место',
    TEXT_NOTIFICATION_FOLDER_MOVED_TO_FOLDER: 'Папка «:name» перемещена в папку «:folder»',
    TEXT_NOTIFICATION_FOLDER_MOVED_TO_TRASH: 'Папка «:name» перемещена в Корзину',
    TEXT_NOTIFICATION_FOLDER_DELETED: 'Папка «:name» была удалена',
    TEXT_NOTIFICATION_FOLDER_EXISTS: 'Папка с именем «:name» уже существует',
    TEXT_NOTIFICATION_FOLDER_CAN_NOT_CREATE: 'Не удалось создать папку «:name»',
    TEXT_NOTIFICATION_FILE_COPIED: 'Файл «:name» скопирован в папку «Файлы»',
    TEXT_NOTIFICATION_FILE_COPIED_TO_FOLDER: 'Файл «:name» скопирован в папку «:folder»',
    TEXT_NOTIFICATION_FILE_MOVED: 'Файл «:name» перемещен в «Файлы»',
    TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH: 'Файл «:name» перемещён в Корзину',
    TEXT_NOTIFICATION_FILE_MOVED_TO_FOLDER: 'Файл «:name» перемещен в «:folder»',
    TEXT_NOTIFICATION_FILE_DELETED: 'Файл «:name» был удален',
    TEXT_NOTIFICATION_FILE_RESTORE: 'Файл «:name» восстановлен',
    TEXT_NOTIFICATION_FILE_UNPUBLISH: 'Публичная ссылка на файл «:name» удалена',
    TEXT_NOTIFICATION_FILE_ADDED_TO_ALBUM: 'Файл добавлен в альбом «:name»',
    TEXT_NOTIFICATION_FILES_ADDED_TO_ALBUM: ':count файла добавлены в альбом «:name»',
    TEXT_NOTIFICATION_PUBLISH_ERROR: 'Не удалось опубликовать файл «:name»',
    TEXT_NOTIFICATION_TRASH_CLEAN: 'Корзина успешно очищена',
    TEXT_NOTIFICATION_FILE_EXCLUDED: 'Файл убран из альбома',
    TEXT_NOTIFICATION_UPLOAD_ERROR: 'Не удалось загрузить файл «:name»',
    TEXT_NOTIFICATION_ALBUM_CREATED: 'Вы создали альбом «:name»',
    TEXT_NOTIFICATION_ALBUM_PUBLISHED: 'Опубликован альбом «:name»',
    TEXT_NOTIFICATION_ALBUM_UNPUBLISHED: 'Теперь альбом «:name» виден только вам',
    TEXT_NOTIFICATION_ALBUM_REMOVED: 'Альбом «:name» был удален',
    TEXT_NOTIFICATION_COVER_CHANGED: 'Обложка изменена',
    TEXT_NOTIFICATION_TITlE_TOO_LONG: 'Название альбома не может быть длиннее 255 символов',
    TEXT_NOTIFICATION_FILE_TITlE_TOO_LONG: 'Название файла не может быть длиннее 255 символов',
    TEXT_NOTIFICATION_LINK_COPIED: 'Ссылка скопирована',
    TEXT_UPLOAD_DIALOG_UPLOAD_COMPLETE: 'Все файлы загружены',
    TEXT_UPLOAD_DIALOG_UPLOAD: 'Загрузка',
    TEXT_UPLOAD_ERROR_FILE_EXIST: 'Уже есть в папке',
    TEXT_NOTIFICATION_FILE_WITHIN_FOLDER_EDITED: 'Не удалось переместить в Корзину. ' +
        'Документ в этой папке редактируется',

    TEXT_EMPTY_PUBLISH_INPUT: 'Поделиться снова',

    TEXT_NOT_ENOUGH_FREE_SPACE: 'Недостаточно свободного места',
    TEXT_NO_FREE_SPACE: 'Осталось 0 байт из 10 ГБ',
    TEXT_RENAME_ERROR_FOLDER_NAME_CAN_NOT_INCLUDE: 'Название папки не может содержать символ «:name»',

    TEST_FILES_PATH: './tests/hermione/files',
    TEST_FILES_HASH_ALGORITHM: 'md5',

    ANDROID_FILES_PATH: '/data/local/tmp',

    // маленькие буквы, т.к. firefox не понимает с большими
    KEY_SHIFT: '\ue008',
    KEY_CTRL: '\ue009'
};

CONSTS.TEST_256_CHAR_NAME = CONSTS.TEST_255_CHAR_NAME + 'e';

module.exports = CONSTS;
