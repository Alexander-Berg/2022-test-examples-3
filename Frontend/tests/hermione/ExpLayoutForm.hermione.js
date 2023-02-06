const assert = require('chai').assert;

const ROOT_MOUNT_DELAY = 20000;
const NIRVANA_DELAY = 3000;
const FILE_LOADING_DELAY = 20000;
const USER_ACTION_DELAY = 2000;

describe('ExpLayoutForm:', function() {
    afterEach(function() {
        return this.browser.yaCheckClientErrors();
    });

    describe('создание', function() {
        hermione.skip.in('firefox', 'multi-file upload doesn\'t work');
        it('с использованием батчевого загрузчика', function() {
            return this.browser
                .openSbs('/experiment/create/layout')
                // Wait until everything is fancy
                .waitForExist('.ExpCreate', ROOT_MOUNT_DELAY)
                // Fill out required fields
                .setValue('[name=title-field] input', 'E2E is a servant to democracy!')
                .setValue('[name=question-field] input', 'Is this a Hermione-created task?')
                // Uploading files semi-manually. Built-in .choseFiles() allows only one file.
                .click('[name=open-batch-loader-modal-btn]')
                .waitForExist('[name=batch-zone]', USER_ACTION_DELAY)
                .then(function() {
                    // Uploads files from CWD to selenium grid - necessary.
                    return Promise.all([
                        'tests/unit/fixtures/layouts/0_0.jpg',
                        'tests/unit/fixtures/layouts/0_1.jpg',
                        'tests/unit/fixtures/layouts/1-0.jpg',
                        'tests/unit/fixtures/layouts/1-1.jpg',
                    ].map((file) => this.uploadFile(file)))
                    // get file paths
                        .then((files) => files.map((file) => file.value))
                        // "enter" them into field - it actually sends keystrokes with paths into fields AFAIK
                        .then((files) => this.addValue('[name=batch-zone] input', files.join('\n')));
                })
                // Wait for upload to complete. TODO this might fail due to S3 timeout
                .waitForExist('[name=layout_0_0] .LayoutsForm-Img', FILE_LOADING_DELAY)
                .waitForExist('[name=layout_0_1] .LayoutsForm-Img', FILE_LOADING_DELAY)
                .waitForExist('[name=layout_1_0] .LayoutsForm-Img', FILE_LOADING_DELAY)
                .waitForExist('[name=layout_1_1] .LayoutsForm-Img', FILE_LOADING_DELAY)
                .click('[name=save-btn]')
                // Wait for experiment creation
                .waitForExist('.MessageList-Card.MessageCard.MessageCard_theme_green', NIRVANA_DELAY)
                .getText('.MessageList-Card.MessageCard.MessageCard_theme_green .MessageBox-Text')
                .then((title) => assert.equal(title, 'Эксперимент создан'));
        });

        hermione.skip.in('firefox', 'multi-file upload doesn\'t work');
        it('одноэкранного эксперимента', function() {
            return this.browser
                .openSbs('/experiment/create/layout')
                .waitForExist('.ExpCreate', ROOT_MOUNT_DELAY)
                .setValue('[name=title-field] input', 'E2E is a servant to democracy!')
                .setValue('[name=question-field] input', 'Is this a Hermione-created task?')
                .then(function() {
                    return Promise.all([
                        'tests/unit/fixtures/layouts/0_0.jpg',
                        'tests/unit/fixtures/layouts/0_1.jpg',
                    ]
                        .map((file) => this.uploadFile(file)))
                        .then((files) => files.map((file) => file.value))
                        .then((files) => this.addValue(
                            '[name=layout_0_0] input',
                            files.join('\n'),
                        ));
                })
                .waitForExist('[name=layout_0_0] .LayoutsForm-Img', FILE_LOADING_DELAY)
                .waitForExist('[name=layout_0_1] .LayoutsForm-Img', FILE_LOADING_DELAY)
                .click('[name=save-btn]')
                .waitForExist('.MessageList-Card.MessageCard.MessageCard_theme_green', NIRVANA_DELAY)
                .getText('.MessageList-Card.MessageCard.MessageCard_theme_green .MessageBox-Text')
                .then((title) => assert.equal(title, 'Эксперимент создан'));
        });

        hermione.skip.in('firefox', 'multi-file upload doesn\'t work');
        it('создание и запуск', async function() {
            const files = [
                'tests/unit/fixtures/layouts/0_0.jpg',
                'tests/unit/fixtures/layouts/0_1.jpg',
            ];

            await this.browser.openSbs('/experiment/create/layout');
            await this.browser.waitForExist('.ExpCreate', ROOT_MOUNT_DELAY);
            await this.browser.setValue('[name=title-field] input', 'E2E is a servant to democracy!');
            await this.browser.setValue('[name=question-field] input', 'Is this a Hermione-created task?');

            const uploadedFiles = await Promise.all(
                files.map(async(file) => {
                    const { value } = await this.browser.uploadFile(file);
                    return value;
                }),
            );

            await this.browser.addValue('[name=layout_0_0] input', uploadedFiles.join('\n'));

            await this.browser.waitForExist('[name=layout_0_0] .LayoutsForm-Img', FILE_LOADING_DELAY);
            await this.browser.waitForExist('[name=layout_0_1] .LayoutsForm-Img', FILE_LOADING_DELAY);
            await this.browser.click('[name=save-and-start-btn]');
            await this.browser.waitForExist('.MessageList-Card.MessageCard', NIRVANA_DELAY);
        });

        hermione.skip.in('firefox', 'multi-file upload doesn\'t work');
        it('многоэкранного эксперимента', function() {
            return this.browser
                .openSbs('/experiment/create/layout')
                .waitForExist('.ExpCreate', ROOT_MOUNT_DELAY)
                .setValue('[name=title-field] input', 'E2E is a servant to democracy!')
                .setValue('[name=question-field] input', 'Is this a Hermione-created task?')
                .click('[name=multiscreen-toggler]')
                .waitForExist('[name=screen_1', USER_ACTION_DELAY)
                .then(function() {
                    return Promise.all([
                        'tests/unit/fixtures/layouts/0_1.jpg',
                        'tests/unit/fixtures/layouts/0_0.jpg',
                    ]
                        .map((file) => this.uploadFile(file)))
                        .then((files) => files.map((file) => file.value))
                        .then((files) => this.addValue(
                            '[name=layout_0_0] input',
                            files.join('\n'),
                        ))
                        .then(() =>
                            this
                                .waitForExist('[name=layout_0_0] .LayoutsForm-Img', FILE_LOADING_DELAY)
                                .waitForExist('[name=layout_0_1] .LayoutsForm-Img', FILE_LOADING_DELAY),
                        );
                })
                .then(function() {
                    return Promise.all([
                        'tests/unit/fixtures/layouts/1-0.jpg',
                        'tests/unit/fixtures/layouts/1-1.jpg',
                    ]
                        .map((file) => this.uploadFile(file)))
                        .then((files) => files.map((file) => file.value))
                        .then((files) => this.addValue(
                            '[name=layout_1_0] input',
                            files.join('\n'),
                        ))
                        .then(() =>
                            this
                                .waitForExist('[name=layout_1_0] .LayoutsForm-Img', FILE_LOADING_DELAY)
                                .waitForExist('[name=layout_1_1] .LayoutsForm-Img', FILE_LOADING_DELAY),
                        );
                })
                .click('[name=save-btn]')
                .waitForExist('.MessageList-Card.MessageCard.MessageCard_theme_green', NIRVANA_DELAY)
                .getText('.MessageList-Card.MessageCard.MessageCard_theme_green .MessageBox-Text')
                .then((title) => assert.equal(title, 'Эксперимент создан'));
        });
    });

    describe('батчевый загрузчик', () => {
        hermione.skip.in('firefox', 'multi-file upload doesn\'t work');
        it('валидирует неправильное количество картинок', function() {
            return this.browser
                .openSbs('/experiment/create/layout')
                // Wait until everything is fancy
                .waitForExist('.ExpCreate', ROOT_MOUNT_DELAY)
                // Fill out required fields
                .setValue('[name=title-field] input', 'E2E is a servant to democracy!')
                .setValue('[name=question-field] input', 'Is this a Hermione-created task?')
                // Uploading files semi-manually. Built-in .choseFiles() allows only one file.
                .click('[name=open-batch-loader-modal-btn]')
                .waitForExist('[name=batch-zone]', USER_ACTION_DELAY)
                .then(function() {
                    // Uploads files from CWD to selenium grid - necessary.
                    return Promise.all([
                        'tests/unit/fixtures/layouts/0_0.jpg',
                        'tests/unit/fixtures/layouts/0_1.jpg',
                        'tests/unit/fixtures/layouts/1-0.jpg',
                    ].map((file) => this.uploadFile(file)))
                    // get file paths
                        .then((files) => files.map((file) => file.value))
                        // "enter" them into field - it actually sends keystrokes with paths into fields AFAIK
                        .then((files) => this.addValue('[name=batch-zone] input', files.join('\n')));
                })
                // Wait for upload to complete. TODO this might fail due to S3 timeout
                .waitForExist('[name=layout_0_0] .LayoutsForm-Img', FILE_LOADING_DELAY)
                .waitForExist('[name=layout_0_1] .LayoutsForm-Img', FILE_LOADING_DELAY)
                .waitForExist('[name=layout_1_0] .LayoutsForm-Img', FILE_LOADING_DELAY)
                .waitForExist('[name=layout-field] .FormFieldError .FormFieldError-Item', USER_ACTION_DELAY)
                .getText('[name=layout-field] .FormField .FormFieldError-Item')
                .then((title) => assert.equal(title, 'На всех экранах должно быть одинаковое количество картинок'));
        });

    });

    it('клонирование эксперимента', function() {
        return this.browser
            .openSbs('/experiment/10961')
            .waitForExist('.ExpMeta-Controls', 5000)
            // TODO: дать кнопке клонирования отдельный класс
            .click('.ExpMeta-Controls .ExpMeta-Control')
            .waitForExist('.ExpLayoutForm-Title')
            .waitUntil(async function() {
                // ждем пока пропадут уведомления
                const notifications = await this.$$('.MessageList-Card');
                return !notifications.length;
            }, 8500)
            .scroll('.ExpLayoutForm-SubmitGroup')
            .click('.ExpLayoutForm-SubmitGroup [name=save-btn]')
            .waitForExist('.MessageList-Card.MessageCard.MessageCard_theme_green', 5000)
            .getText('.MessageList-Card.MessageCard.MessageCard_theme_green .MessageBox-Text')
            .then((title) => {
                assert.equal(title, 'Эксперимент создан');
            });
    });
});
