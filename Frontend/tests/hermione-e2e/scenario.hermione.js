const assert = require('chai').assert;
const url = require('url');
const path = require('path');
const qs = require('query-string');

const delay = require('./delay-config');
const { getTextWithDate, getRandomString, getMDSHostName } = require('./utils');

const LOGIN = process.env.LOGIN;
const PASSWORD = process.env.PASS;

describe('Сценарные эксперименты:', function() {
    it('создание эксперимента с медиа-файлами', function() {
        return this.browser
            .passportAuth(LOGIN, PASSWORD)
            .url('/experiment/create/scenario')
            .waitForExist('.ExpCreate', delay.ROOT_MOUNT)
            .setValue('[name=title-field] input', getTextWithDate('E2E is a servant to democracy!'))
            .setValue('[name=scenario-field] textarea', 'Is this a Hermione-created task?')
            .click('.ScenariosForm-EmptyButton_type_media')
            .waitForExist('.FormItem [type=file]', delay.USER_ACTION)
            .then(function() {
                return this.uploadFile('tests/unit/fixtures/scenario/test-audio.mp3')
                    .then((file) => (
                        this
                            .setValue('.FormItem:first-child [type=file]', file.value)
                            .setValue('.FormItem:last-child [type=file]', file.value)
                            .setValue('.FormItem:first-child [type=text]', getRandomString())
                            .setValue('.FormItem:last-child [type=text]', getRandomString())
                            .waitUntil(async() => {
                                // ждем пока пропадут спинеры, т.е. закончится загрузка файлов
                                const loaders = await this.$$('.FormItem .SpinnerWrapper-Overlay_active');
                                return !loaders.length;
                            }, delay.FILE_LOADING)
                    ));
            })
            .setValue('[name=question-field] textarea', 'This is Hermione\'s question')
            .click('[name=save-and-start-btn]')
            .waitForExist('.MessageList-Card.MessageCard.MessageCard_theme_green', delay.NIRVANA)
            .compareInnerText('.MessageList-Card.MessageCard.MessageCard_theme_green .MessageBox-Text', 'Эксперимент запущен');
    });

    it('создание эксперимента в режиме А с figma прототипом', function() {
        const FIGMA_PROTO = 'https://www.figma.com/file/XK0KTZ0Vll0ilWSWwa09PU/Zero-suggest-new?node-id=0%3A1';

        return this.browser
            .passportAuth(LOGIN, PASSWORD)
            .url('/experiment/create/scenario')
            .waitForExist('.ExpCreate', delay.ROOT_MOUNT)
            .setValue('[name=title-field] input', getTextWithDate('E2E mode A with Figma!'))
            .setValue('[name=scenario-field] textarea', 'Is this a Hermione-created task?')
            .click('[name=experiment-mode] [value=a-mode]')
            .click('.ScenariosForm-EmptyButton_type_figma')
            .setValue('.FormItem-FieldContent_title [type=text]', getRandomString())
            .setValue('.FormItem-FieldContent_link [type=text]', FIGMA_PROTO)
            .waitForExist('.PollForm-Item_type_question textarea', delay.USER_ACTION)
            .setValue('.PollForm-Item_type_question textarea', 'This is the Hermione\'s question')
            .click('[name=save-and-start-btn]')
            .waitForExist('.MessageList-Card.MessageCard.MessageCard_theme_green', delay.NIRVANA)
            .compareInnerText('.MessageList-Card.MessageCard.MessageCard_theme_green .MessageBox-Text', 'Эксперимент запущен');
    });

    it('загрузка файлов прототипов (zip и html)', function() {
        const MDS_HOST = getMDSHostName();
        const HTML_PROTO_FILE_NAME = 'html-proto.html';
        const ZIP_PROTO_FILE_NAME = 'zip-proto.zip';

        return this.browser
            .passportAuth(LOGIN, PASSWORD)
            .url('/experiment/create/scenario')
            .waitForExist('.ScenariosForm', delay.ROOT_MOUNT)
            .click('.ScenariosForm-EmptyButton_type_file')
            .waitForExist('.FormItem [type=file]', delay.USER_ACTION)
            .then(function() {
                return Promise.all([
                    `tests/unit/fixtures/scenario/${HTML_PROTO_FILE_NAME}`,
                    `tests/unit/fixtures/scenario/${ZIP_PROTO_FILE_NAME}`,
                ].map((file) => this.uploadFile(file)))
                    .then((files) => {
                        return files.reduce(
                            (browser, { value }, index) => browser.setValue(`.FormItem:nth-child(${index + 1}) [type=file]`, value),
                            this,
                        )
                            .waitUntil(async() => {
                                // ждем пока пропадут спинеры, т.е. закончится загрузка файлов
                                const loaders = await this.$$('.FormItem .SpinnerWrapper-Overlay_active');
                                return !loaders.length;
                            }, delay.FILE_LOADING);
                    });
            })
            .then(function() {
                return [HTML_PROTO_FILE_NAME, ZIP_PROTO_FILE_NAME]
                    .reduce((browser, fileName, index) => {
                        return browser
                            .compareInnerText(
                                `.FormItem:nth-child(${index + 1}) .FormItem-UploadHolder span`,
                                fileName,
                                'В холдере должно быть имя загруженного файла',
                            )
                            .getAttribute(`.FormItem:nth-child(${index + 1}) .FormItem-Preview .link`, 'href')
                            .then((previewLink) => {
                                const parsedUrl = url.parse(previewLink);
                                const extName = path.extname(fileName);
                                assert.strictEqual(parsedUrl.host, MDS_HOST, 'Должена быть ссылка на корректный хост MDS');
                                assert.isTrue(
                                    previewLink.endsWith('.html'),
                                    `При загрузке ${extName} файла ссылка предпросмотра должна быть на .html файл`,
                                );
                                return this.browser;
                            });
                    }, this);
            });
    });

    it('добавление ссылок с test-id', function() {
        const SERP_LINK = 'https://yandex.ru/search/?text=bmw';
        const TEST_ID = 12345;

        return this.browser
            .passportAuth(LOGIN, PASSWORD)
            .url('/experiment/create/scenario')
            .waitForExist('.ScenariosForm', delay.ROOT_MOUNT)
            .click('.ScenariosForm-EmptyButton_type_testId')
            .waitForExist('.FormItem [type=text]', delay.USER_ACTION)
            .setValue('.FormItem:first-child .FormItem-FieldContent_link input', SERP_LINK)
            .setValue('.FormItem:first-child .FormItem-FieldContent_testId input', TEST_ID)
            .getAttribute('.FormItem-Preview .link', 'href')
            .then((previewLink) => {
                const queryParams = qs.parse(url.parse(previewLink).query);
                assert.strictEqual(
                    queryParams['test-id'], String(TEST_ID),
                    'Переданный test-id должен корректно записывать в гет-параметр',
                );
                return this.browser;
            });
    });

    it('загрузка медиа-файлов', function() {
        const MDS_HOST = getMDSHostName();
        const VIDEO_FILE_NAME = 'test-video.mp4';
        const AUDIO_FILE_NAME = 'test-audio.mp3';
        const IMAGE_FILE_NAME = 'test-image.png';

        return this.browser
            .passportAuth(LOGIN, PASSWORD)
            .url('/experiment/create/scenario')
            .waitForExist('.ScenariosForm', delay.ROOT_MOUNT)
            .click('.ScenariosForm-EmptyButton_type_media')
            .waitForExist('.FormItem [type=file]', delay.USER_ACTION)
            .click('.ScenariosForm-FormAddItemButton')
            .then(function() {
                return Promise.all([
                    `tests/unit/fixtures/scenario/${VIDEO_FILE_NAME}`,
                    `tests/unit/fixtures/scenario/${AUDIO_FILE_NAME}`,
                    `tests/unit/fixtures/scenario/${IMAGE_FILE_NAME}`,
                ].map((file) => this.uploadFile(file)))
                    .then((files) => {
                        return files.reduce(
                            (browser, { value }, index) => browser.setValue(`.FormItem:nth-child(${index + 1}) [type=file]`, value),
                            this,
                        )
                            .waitUntil(async() => {
                                // ждем пока пропадут спинеры, т.е. закончится загрузка файлов
                                const loaders = await this.$$('.FormItem .SpinnerWrapper-Overlay_active');
                                return !loaders.length;
                            }, delay.FILE_LOADING);
                    });
            })
            .then(function() {
                return [VIDEO_FILE_NAME, AUDIO_FILE_NAME, IMAGE_FILE_NAME]
                    .reduce((browser, fileName, index) => {
                        return browser
                            .compareInnerText(
                                `.FormItem:nth-child(${index + 1}) .FormItem-UploadHolder span`,
                                fileName,
                                'В холдере должно быть имя загруженного файла',
                            )
                            .getAttribute(`.FormItem:nth-child(${index + 1}) .FormItem-Preview .link`, 'href')
                            .then((previewLink) => {
                                const parsedUrl = url.parse(previewLink);
                                const extName = path.extname(fileName);

                                assert.strictEqual(parsedUrl.host, MDS_HOST, 'Должена быть ссылка на корректный хост MDS');
                                assert.isTrue(
                                    previewLink.endsWith('.html'),
                                    `При загрузке ${extName} файла ссылка предпросмотра должна быть на .html файл`,
                                );
                                return this.browser;
                            });
                    }, this);
            });
    });
});
