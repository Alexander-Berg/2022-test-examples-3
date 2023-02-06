import {
    isMobileBottomDirectEnabled,
    isMobileBottomDirectVisible
} from '../../../../../src/store/selectors/mobile-bottom-direct';

describe('mobile bottom direct selectors', () => {
    describe('isMobileBottomDirectEnabled - проверка признака маунта компонента рекламы под контентом для мобильных тач-устройств', () => {
        let testState;

        beforeEach(() => {
            testState = {
                ua: { isMobile: true },
                environment: {
                    noAdv: false
                },
                resources: {},
                rootResourceId: 'resource'
            };
            global.APP = false;
        });

        afterEach(() => {
            global.APP = false;
        });

        it('признак равен true, если нет доступного ресурса, для бесплатных тач-пользователей', () => {
            expect(isMobileBottomDirectEnabled(testState)).toEqual(true);
        });

        it('признак равен true для мобильных устройств', () => {
            expect(isMobileBottomDirectEnabled(testState)).toEqual(true);

            testState.ua.isMobile = false;
            expect(isMobileBottomDirectEnabled(testState)).toEqual(false);
        });

        it('признак равен false, если ресурс - картинка с превью или загруженное без ошибок видео доступное для видео плеера', () => {
            const viewResults = [
                {
                    resource: null,
                    result: true
                },
                {
                    resource: undefined,
                    result: true
                },
                {
                    resource: { type: 'dir' },
                    result: true
                },
                {
                    resource: { errorCode: 404 },
                    result: true
                },
                {
                    resource: {
                        blocked: true,
                        errorCode: 123
                    },
                    result: true
                },
                {
                    resource: {
                        blocked: false,
                        meta: { mediatype: 'audio' }
                    },
                    result: true
                },
                {
                    resource: {},
                    result: true
                },
                {
                    resource: {
                        meta: { hasPreview: true }
                    },
                    result: true
                },
                {
                    resource: {
                        blocked: true,
                        meta: { mediatype: 'image' }
                    },
                    result: true
                },
                {
                    resource: {
                        blocked: false,
                        meta: { mediatype: 'image' }
                    },
                    result: true
                },
                {
                    resource: {
                        blocked: false,
                        meta: { mediatype: 'image', hasPreview: true }
                    },
                    result: false
                },
                {
                    resource: {
                        meta: { mediatype: 'video' },
                        isAvialableForVideoPlayer: true
                    },
                    result: false
                }
            ];

            viewResults.forEach(({ resource, result }) => {
                testState.resources = { resource };
                expect(isMobileBottomDirectEnabled(testState)).toEqual(result);
            });
        });

        it('признак равен false для платных пользователей', () => {
            expect(isMobileBottomDirectEnabled(testState)).toEqual(true);

            testState.environment.noAdv = true;
            expect(isMobileBottomDirectEnabled(testState)).toEqual(false);
        });

        it('признак равен false в случае, если паблик открыт в мобильном приложении через webview', () => {
            expect(isMobileBottomDirectEnabled(testState)).toEqual(true);

            global.APP = true;
            expect(isMobileBottomDirectEnabled(testState)).toEqual(false);
        });
    });

    describe('isMobileBottomDirectVisible - проверка признака видимости рекламы под контентом для мобильных тач-устройств', () => {
        let testState;

        beforeEach(() => {
            testState = {
                resources: {
                    resource: { completed: false }
                },
                currentResourceId: 'resource'
            };
        });

        it('признак равен true, если ресурс не найден', () => {
            expect(isMobileBottomDirectVisible(testState)).toEqual(false);

            testState.resources = { resource: null };
            expect(isMobileBottomDirectVisible(testState)).toEqual(true);
        });

        it('признак равен true, если ресурс полностью загружен', () => {
            testState.resources.resource.completed = true;
            expect(isMobileBottomDirectVisible(testState)).toEqual(true);
        });
    });
});
