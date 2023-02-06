const _ = require('lodash');

const validator = require('../../../../src/shared/validators/scenario');
const pollErrorMessages = require('../../../../src/shared/validators/poll').ERROR_MESSAGES;
const { BATCH_SCENARIO_VARIANTS } = require('./fixtures');

describe('scenario validator', function() {
    describe('title required', function() {
        it('Должен возвращать успешный результат в случае наличия непустого значения', function() {
            const result = validator.fields.title.required('Заголовок эксперимента');
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку валидации в случае пустого значения', function() {
            const result = validator.fields.title.required('');
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });
    });

    describe('scenario validateScenarioText', function() {
        it('Должен возвращать успешный результат в случае наличия непустого сценария', function() {
            const result = validator.validateScenarioText({ scenario: 'Сценарий эксперимента' });
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку валидации в случае пустого сценария', function() {
            const result = validator.validateScenarioText({});
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });

        it('Должен возвращать успешный результат есть в батч режиме есть scenario в каждом из вариантов', function() {
            const dataStub = {
                batchMode: true,
                variants: {
                    pages: [
                        { scenario: 'Просмотрите оба варианта главной страницы Яндекса' },
                        { scenario: 'Нажмите кнопку на главной страницы Яндекса' },
                    ],
                },
            };
            const result = validator.validateScenarioText(dataStub);
            assert.strictEqual(result.isValid, true);
        });

        it('Должен возвращать ошибку валидации есть в батч режиме нет scenario в одном из вариантов', function() {
            const dataStub = {
                batchMode: true,
                variants: {
                    pages: [{ a: 1 }, { b: 2 }],
                },
            };
            const result = validator.validateScenarioText(dataStub);
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
        });
    });

    describe('question required', function() {
        it('Должен возвращать успешный результат в случае наличия непустого вопроса в режиме "sbs"', function() {
            const val = [{
                data: { question: 'Описание эксперимента' },
            }];
            const result = validator.fields.poll.validatePoll(val, 'sbs');
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку валидации в случае пустого вопроса в режиме "sbs"', function() {
            const val = [{
                data: { question: '' },
            }];
            const result = validator.fields.poll.validatePoll(val, 'sbs');
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });
    });

    describe('validateUrl', function() {
        it('Должен возвращать true в случае прохождения валидации URL-а Figma', function() {
            const result = validator.validateUrl('figma', 'https://www.figma.com');
            assert.strictEqual(result.isValid, true);
        });

        it('Должен возвращать true  в случае прохождения валидации URL-а тестида', function() {
            const result = validator.validateUrl('testId', 'https://yandex.ru/images/');
            assert.strictEqual(result.isValid, true);
        });

        it('Должен возвращать false в случае неправильного URL-а Figma', function() {
            const result = validator.validateUrl('figma', 'https://www.fima.com');
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });

        it('Должен возвращать false в случае неправильного URL-а тестида', function() {
            const result = validator.validateUrl('testId', 'https://yandex.ru/img/');
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });

        it('Должен возвращать false если URL-а нет в списке разрешенных', function() {
            const result = validator.validateUrl('testId', 'https://il7test.yandex.ru/img/');
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });

        it('Должен возвращать true  если URL-а нет в списке разрешенных, но эксперимент на коллегах', function() {
            const result = validator.validateUrl('testId', 'https://il7test.yandex.ru/img/', 'colleagues');
            assert.strictEqual(result.isValid, true);
        });

        it('Должен возвращать false в случае пустого URL для Figma', function() {
            const result = validator.validateUrl('figma', '');
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });

        it('Должен возвращать false в случае пустого URL для тестиды', function() {
            const result = validator.validateUrl('testId', '');
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });

        it('Должен возвращать false в произвольных ссылках, если указан внутренний сервис yandex', function() {
            const result = validator.validateUrl('link', 'https://github.yandex-team.ru/mm-interfaces/samadhi/');
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });

        it('Должен возвращать false в произвольных ссылках, если ссылка начинается с yandex.', function() {
            const result = validator.validateUrl('link', 'https://yandex.ru/images/search?text=%D0%BA%D0%BE%D1%82%D0%B8%D0%BA%D0%B8&exp_flags=images_viewer_editor_new_label%3D14%3Bimages_viewer_editor%3Breact_enable%3Bimages_touch_reactor%3Bimages_touch_viewer2_new_controls_design%3D2%3Bimages_scrollable_touch_viewer_config%3Donboarding%2Cpanel%2Cdirect-snippet%2Ctags%2Crim%2Csizes%2Ccollections%2Csites%2Cmarket%3Bimages_viewer_editor_onboarding%3Dd%3A7%2Cm%3A2%2Cmax%3A60&exp_flags=images_viewer_editor_onboarding_cm2_treshold%3D0.21&exp_flags=images_viewer_editor_onboarding_swipe_count%3D5&exp_flags=images_viewer_editor_onboarding_timer%3D1&redircnt=1567413927.1');
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });
    });
    describe('overlap', function() {
        it('Должен возвращать успешный результат в случае режима "default" и пустого значения', function() {
            const result = validator.fields.overlap.validate({ mode: 'default' });
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать успешный результат в случае режима "edit" и значения "1"', function() {
            const result = validator.fields.overlap.validate({ mode: 'edit', value: '1' });
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать успешный результат в случае режима "edit" и значения "100"', function() {
            const result = validator.fields.overlap.validate({ mode: 'edit', value: '100' });
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать успешный результат в случае режима "edit" и значения между "1" и "100"', function() {
            const result = validator.fields.overlap.validate({ mode: 'edit', value: '50' });
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать успешный результат в случае режима "edit" и отсутствия значения', function() {
            const result = validator.fields.overlap.validate({ mode: 'edit', value: '' });
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });

        it('Должен возвращать успешный результат в случае режима "edit" и значения меньше 1', function() {
            const result = validator.fields.overlap.validate({ mode: 'edit', value: '0' });
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });

        it('Должен возвращать успешный результат в случае режима "edit" и значения больше 200', function() {
            const result = validator.fields.overlap.validate({ mode: 'edit', value: '201' });
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });
    });

    describe('validateBatchPages', function() {
        it('Должен возвращать успешный результат если эксперимент не батч', function() {
            const result = validator.validateBatchPages({ batchMode: false });
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку валидации если количество экранов отличается от количества списков прототипов', function() {
            const expParams = {
                batchMode: true,
                variants: {
                    items: [
                        { urls: [{ url: 'url' }, { url: 'url' }, { url: 'url' }] },
                        { urls: [{ url: 'url' }, { url: 'url' }, { url: 'url' }] },
                    ],
                    pages: [{ name: 1 }, { name: 2 }],
                    systems: [{ name: 1 }, { name: 2 }],
                },
            };

            const result = validator.validateBatchPages(expParams);

            assert.isFalse(result.isValid);
            assert.exists(result.errorsMessage);
        });

        it('Должен возвращать ошибку валидации если количество систем отличается от количества переданных ссылок', function() {
            const expParams = {
                batchMode: true,
                variants: {
                    items: [
                        { urls: [{ url: 'url' }, { url: 'url' }, { url: 'url' }] },
                        { urls: [{ url: 'url' }, { url: 'url' }, { url: 'url' }] },
                    ],
                    pages: [{ name: 1 }, { name: 2 }, { name: 3 }],
                    systems: [{ name: 1 }, { name: 2 }],
                },
            };

            const result = validator.validateBatchPages(expParams);

            assert.isFalse(result.isValid);
            assert.exists(result.errorsMessage);
        });

        it('Должен возвращать ошибку валидации если указан тип testId, но не у всех прототипов есть testId', function() {
            const expParams = {
                batchMode: true,
                variants: {
                    items: [
                        { urls: [{ url: 'url', testId: '123' }, { url: 'url', testId: '123' }] },
                        { urls: [{ url: 'url', testId: '123' }, { url: 'url' }] },
                    ],
                    type: 'testId',
                    pages: [{ name: 1 }, { name: 2 }],
                    systems: [{ name: 1 }, { name: 2 }],
                },
            };

            const result = validator.validateBatchPages(expParams);

            assert.isFalse(result.isValid);
            assert.exists(result.errorsMessage);
        });
    });

    describe('validateVariantType', function() {
        it('Должен возвращать успешный результат в случае валидного типа', function() {
            const result = validator.validateVariantType('link');
            assert.isTrue(result.isValid);
        });

        it('Должен возвращать ошибку валидации в случае не валидного типа', function() {
            const result = validator.validateVariantType('123');
            assert.isFalse(result.isValid);
        });
    });

    describe('validateSystemIds', function() {
        describe('батчевые сценарные эксперименты', function() {
            let variants;

            beforeEach(function() {
                variants = _.cloneDeep(BATCH_SCENARIO_VARIANTS);
            });

            it('Должен возвращать успешный результат, если для систем не заданы systemId', function() {
                const result = validator.validateSystemIds(variants, true);

                assert.isTrue(result.isValid);
            });

            it('Должен возвращать ошибку валидации, если systemId заданы, но не для всех систем', function() {
                delete variants.systems[1].systemId;
                const result = validator.validateSystemIds(variants, true);

                assert.isFalse(result.isValid);
                assert.exists(result.errorsMessage);
            });

            it('Должен возвращать ошибку валидации, если systemId заданы, но pageId заданы не для каждого элемента items', function() {
                delete variants.items[1].pageId;
                const result = validator.validateSystemIds(variants, true);

                assert.isFalse(result.isValid);
                assert.exists(result.errorsMessage);
            });

            it('Должен возвращать ошибку валидации, если systemId заданы, но не для всех элементов items.urls', function() {
                delete variants.items[1].urls[1].systemId;
                const result = validator.validateSystemIds(variants, true);

                assert.isFalse(result.isValid);
                assert.exists(result.errorsMessage);
            });

            it('Должен возвращать ошибку валидации, если systemId заданы, но pageId заданы не для каждого элемента pages', function() {
                delete variants.pages[1].pageId;
                const result = validator.validateSystemIds(variants, true);

                assert.isFalse(result.isValid);
                assert.exists(result.errorsMessage);
            });
        });
    });

    describe('validateSingleFrameWithVideoStreamSync', function() {
        it('Должен возвращать ошибку валидации, если установлен флаг, но тип систем – не videostream', function() {
            const result = validator.validateSingleFrameWithVideoStreamSync('link', true);

            assert.isFalse(result.isValid);
            assert.exists(result.errorsMessage);
        });

        it('Должен возвращать успешный результат, если флаг задан вместе с типом систем videostream', function() {
            const result = validator.validateSingleFrameWithVideoStreamSync('videostream', true);

            assert.isTrue(result.isValid);
        });

        it('Должен возвращать успешный результат, если флаг не задан', function() {
            const result = validator.validateSingleFrameWithVideoStreamSync('media');

            assert.isTrue(result.isValid);
        });
    });

    describe('validateMultipartPoll', function() {
        it('Должен возвращать ошибку валидации, если режим эксперимента – a-mode и опрос пуст', function() {
            const questionGroups = [
                {
                    poll: [],
                },
            ];
            const result = validator.fields.multipartPoll.validateMultipartPoll(questionGroups, 'a-mode');

            assert.strictEqual(result.isValid, false);
            assert.strictEqual(result.errorsMessage, pollErrorMessages.MULTIPART_POLL_EMPTY);
        });

        it('Должен возвращать успешный результат, если режим эксперимента – a-mode и опрос заполнен', function() {
            const questionGroups = [
                {
                    poll: [
                        {
                            type: 'question',
                        },
                    ],
                },
            ];
            const result = validator.fields.multipartPoll.validateMultipartPoll(questionGroups, 'a-mode');

            assert.strictEqual(result.isValid, true);
        });

        it('Должен возвращать ошибку валидации, если режим эксперимента – ab-mode и опрос пуст', function() {
            const questionGroups = [
                {
                    poll: [],
                },
            ];
            const result = validator.fields.multipartPoll.validateMultipartPoll(questionGroups, 'ab-mode');

            assert.strictEqual(result.isValid, false);
            assert.strictEqual(result.errorsMessage, pollErrorMessages.MULTIPART_POLL_EMPTY);
        });

        it('Должен возвращать успешный результат, если режим эксперимента – ab-mode и опрос заполнен', function() {
            const questionGroups = [
                {
                    poll: [
                        {
                            type: 'question',
                        },
                    ],
                },
            ];
            const result = validator.fields.multipartPoll.validateMultipartPoll(questionGroups, 'ab-mode');

            assert.strictEqual(result.isValid, true);
        });

        it('Должен возвращать ошибку валидации, если режим эксперимента – sbs и не все вопросы заполнены', function() {
            const questionGroups = [
                {
                    poll: [
                        {
                            type: 'question',
                            data: {
                                question: 'q1',
                            },
                        },
                    ],
                },
                {
                    poll: [
                        {
                            type: 'question',
                            data: {
                                question: 'q1',
                            },
                        },
                        {
                            type: 'question',
                            data: {},
                        },
                    ],
                },
            ];
            const result = validator.fields.multipartPoll.validateMultipartPoll(questionGroups, 'sbs');

            assert.strictEqual(result.isValid, false);
            assert.strictEqual(result.errorsMessage, validator.ERROR_MESSAGES.pollQuestionEmpty);
        });

        it('Должен возвращать успешный результат, если режим эксперимента – sbs и все вопросы заполнены', function() {
            const questionGroups = [
                {
                    poll: [
                        {
                            type: 'question',
                            data: {
                                question: 'q1',
                            },
                        },
                    ],
                },
                {
                    poll: [
                        {
                            type: 'question',
                            data: {
                                question: 'q1',
                            },
                        },
                        {
                            type: 'question',
                            data: {
                                question: 'q2',
                            },
                        },
                    ],
                },
            ];
            const result = validator.fields.multipartPoll.validateMultipartPoll(questionGroups, 'sbs');

            assert.strictEqual(result.isValid, true);
        });
    });
});
