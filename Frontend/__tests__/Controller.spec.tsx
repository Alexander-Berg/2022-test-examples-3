import { MultipageController } from '../Controller';
import { ESwipeSide, ESwipeState } from '../interfaces';
import { Config } from '../../utils/config';
import * as emitErrorUtil from '../../utils/emitError';

import { mockWindow, navigator, restoreDom, restoreWindow, roots } from '../../__tests__/tests-lib';

const urls = [
    {
        originalUrl: 'https://original-host1.ru/article/1',
        displayUrl: 'https://yandex.ru/turbo?text=test_news1',
        frameUrl: 'https://yandex.ru/turbo?text=test_news1&some-more-data',
    },
    {
        originalUrl: 'https://original-host2.ru/article/2',
        displayUrl: 'https://yandex.ru/turbo?text=test_news2',
        frameUrl: 'https://yandex.ru/turbo?text=test_news2&some-more-data',
    },
    {
        originalUrl: 'https://original-host3.ru/article/3',
        displayUrl: 'https://yandex.ru/turbo?text=test_news3',
        frameUrl: 'https://yandex.ru/turbo?text=test_news3&some-more-data',
    },
];

describe('Турбо-оверлей', () => {
    describe('Контроллер multipage', () => {
        let getFrameSpy: jest.SpyInstance;
        let getFramesContainerNodeSpy: jest.SpyInstance;
        let setMultipageSpy: jest.SpyInstance;
        let setTitleSpy: jest.SpyInstance;
        let moveFramesContainerSpy: jest.SpyInstance;
        let controller: MultipageController;

        const mockFrameInstance = {
            move: jest.fn(),
            isLoaded: jest.fn().mockReturnValue(true),
        };
        const mockFramesContainerNode = document.createElement('div');
        const root = roots.multipage;

        beforeAll(() => {
            mockWindow();
            getFrameSpy = jest.spyOn(root, 'getFrame');
            getFrameSpy.mockReturnValue(mockFrameInstance);
            getFramesContainerNodeSpy = jest.spyOn(root, 'getFramesContainerNode');
            getFramesContainerNodeSpy.mockReturnValue(mockFramesContainerNode);
            setMultipageSpy = jest.spyOn(root, 'setMultipage');
            setTitleSpy = jest.spyOn(root, 'setTitle');
            moveFramesContainerSpy = jest.spyOn(root, 'moveFramesContainer');
        });
        beforeEach(() => {
            controller = new MultipageController({
                getRootInstance: () => root,
                navigator,
            });
        });
        afterEach(() => {
            controller.reset();
            jest.clearAllMocks();
        });
        afterAll(() => {
            restoreWindow();
            jest.restoreAllMocks();
        });

        describe('Устанавливает верную шапку и заголовок', () => {
            it('Без заданных index и title', () => {
                controller.init(new Config({ urls }));

                expect(setMultipageSpy).toHaveBeenCalledTimes(1);
                expect(setMultipageSpy).toHaveBeenCalledWith({
                    pagesCount: 3,
                    currentIndex: 0,
                    title: [
                        'original-host1.ru',
                        'original-host2.ru',
                        'original-host3.ru',
                    ],
                });
                expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                expect(moveFramesContainerSpy).toHaveBeenCalledWith(-0);

                expect(controller.overlayIndex).toBe(0);
            });

            it('С заданным index и не заданным title', () => {
                controller.init(new Config({
                    urls,
                    index: 1,
                }));

                expect(setMultipageSpy).toHaveBeenCalledTimes(1);
                expect(setMultipageSpy).toHaveBeenCalledWith({
                    pagesCount: 3,
                    currentIndex: 1,
                    title: [
                        'original-host1.ru',
                        'original-host2.ru',
                        'original-host3.ru',
                    ],
                });
                expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                expect(moveFramesContainerSpy).toHaveBeenCalledWith(-100);

                expect(controller.overlayIndex).toBe(1);
            });

            it('С заданным title и не заданным index', () => {
                controller.init(new Config({
                    urls,
                    title: 'test-title',
                }));

                expect(setMultipageSpy).toHaveBeenCalledTimes(1);
                expect(setMultipageSpy).toHaveBeenCalledWith({
                    pagesCount: 3,
                    currentIndex: 0,
                    title: 'test-title',
                });
                expect(setTitleSpy).toHaveBeenCalledTimes(1);
                expect(setTitleSpy).toHaveBeenCalledWith('test-title');
                expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                expect(moveFramesContainerSpy).toHaveBeenCalledWith(-0);

                expect(controller.overlayIndex).toBe(0);
            });

            it('С заданными index и title', () => {
                controller.init(new Config({
                    urls,
                    index: 2,
                    title: 'test-title2',
                }));

                expect(setMultipageSpy).toHaveBeenCalledTimes(1);
                expect(setMultipageSpy).toHaveBeenCalledWith({
                    pagesCount: 3,
                    currentIndex: 2,
                    title: 'test-title2',
                });
                expect(setTitleSpy).toHaveBeenCalledTimes(1);
                expect(setTitleSpy).toHaveBeenCalledWith('test-title2');
                expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                expect(moveFramesContainerSpy).toHaveBeenCalledWith(-200);

                expect(controller.overlayIndex).toBe(2);
            });
        });

        describe('Перелистывание страницы по свайпу', () => {
            let changePageSpy: jest.SpyInstance;
            let screenWidthSpy: jest.SpyInstance;

            beforeAll(() => {
                changePageSpy = jest.spyOn(navigator, 'changePage');
                screenWidthSpy = jest.spyOn(window.screen, 'width', 'get');
                screenWidthSpy.mockReturnValue(1000);
            });
            beforeEach(restoreDom);
            afterAll(() => {
                changePageSpy.mockRestore();
                screenWidthSpy.mockRestore();
            });

            describe('Вправо', () => {
                it('При резком движении', () => {
                    controller.init(new Config({ urls, index: 1 }));
                    jest.clearAllMocks();

                    controller.handleSwipe({
                        state: ESwipeState.end,
                        value: -100,
                        smooth: false,
                        side: ESwipeSide.right,
                        historyNavigateSwipe: false,
                        historyNavigateSwipeEnd: false,
                        inSlider: true,
                    });

                    expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                    expect(moveFramesContainerSpy).toHaveBeenCalledWith(-200);

                    // Имитируем окончание анимации перелистывания
                    const event = new Event('transitionend');
                    mockFramesContainerNode.dispatchEvent(event);

                    expect(setMultipageSpy).toHaveBeenCalledTimes(1);
                    expect(setMultipageSpy).toHaveBeenCalledWith({ currentIndex: 2 });
                    expect(setTitleSpy).toHaveBeenCalledTimes(0);
                    expect(changePageSpy).toHaveBeenCalledTimes(1);
                    expect(changePageSpy).toHaveBeenCalledWith({
                        url: 'https://yandex.ru/turbo?text=test_news3&some-more-data&new_overlay=1&check_swipe=1',
                        orig: 'https://original-host3.ru/article/3',
                        fromUrl: 'https://yandex.ru/turbo?text=test_news2&some-more-data&new_overlay=1&check_swipe=1',
                    }, 'https://yandex.ru/turbo?text=test_news3&some-more-data&new_overlay=1&check_swipe=1');

                    expect(controller.overlayIndex).toBe(2);
                });

                it('При плавном движении более, чем на половину страницы', () => {
                    controller.init(new Config({ urls, index: 1 }));
                    jest.clearAllMocks();

                    controller.handleSwipe({
                        state: ESwipeState.end,
                        value: -501,
                        smooth: true,
                        side: ESwipeSide.right,
                        historyNavigateSwipe: false,
                        historyNavigateSwipeEnd: false,
                        inSlider: true,
                    });

                    expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                    expect(moveFramesContainerSpy).toHaveBeenCalledWith(-200);

                    const event = new Event('transitionend');
                    mockFramesContainerNode.dispatchEvent(event);

                    expect(setMultipageSpy).toHaveBeenCalledTimes(1);
                    expect(setMultipageSpy).toHaveBeenCalledWith({ currentIndex: 2 });
                    expect(setTitleSpy).toHaveBeenCalledTimes(0);
                    expect(changePageSpy).toHaveBeenCalledTimes(1);
                    expect(changePageSpy).toHaveBeenCalledWith({
                        url: 'https://yandex.ru/turbo?text=test_news3&some-more-data&new_overlay=1&check_swipe=1',
                        orig: 'https://original-host3.ru/article/3',
                        fromUrl: 'https://yandex.ru/turbo?text=test_news2&some-more-data&new_overlay=1&check_swipe=1',
                    }, 'https://yandex.ru/turbo?text=test_news3&some-more-data&new_overlay=1&check_swipe=1');

                    expect(controller.overlayIndex).toBe(2);
                });

                it('При плавном движении менее, чем на половину страницы, не происходит', () => {
                    controller.init(new Config({ urls, index: 1 }));
                    jest.clearAllMocks();

                    controller.handleSwipe({
                        state: ESwipeState.end,
                        value: -500,
                        smooth: true,
                        side: ESwipeSide.right,
                        historyNavigateSwipe: false,
                        historyNavigateSwipeEnd: false,
                        inSlider: true,
                    });

                    expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                    expect(moveFramesContainerSpy).toHaveBeenCalledWith(-100);

                    expect(setMultipageSpy).toHaveBeenCalledTimes(0);
                    expect(setTitleSpy).toHaveBeenCalledTimes(0);
                    expect(changePageSpy).toHaveBeenCalledTimes(0);

                    expect(controller.overlayIndex).toBe(1);
                });

                it('При активной последней странице не происходит', () => {
                    controller.init(new Config({ urls, index: 2 }));
                    jest.clearAllMocks();

                    controller.handleSwipe({
                        state: ESwipeState.end,
                        value: -100,
                        smooth: false,
                        side: ESwipeSide.right,
                        historyNavigateSwipe: false,
                        historyNavigateSwipeEnd: false,
                        inSlider: true,
                    });

                    expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                    expect(moveFramesContainerSpy).toHaveBeenCalledWith(-200);

                    expect(setMultipageSpy).toHaveBeenCalledTimes(0);
                    expect(setTitleSpy).toHaveBeenCalledTimes(0);
                    expect(changePageSpy).toHaveBeenCalledTimes(0);

                    expect(controller.overlayIndex).toBe(2);
                });
            });

            describe('Влево', () => {
                it('При резком движении', () => {
                    controller.init(new Config({ urls, index: 1 }));
                    jest.clearAllMocks();

                    controller.handleSwipe({
                        state: ESwipeState.end,
                        value: 100,
                        smooth: false,
                        side: ESwipeSide.left,
                        historyNavigateSwipe: false,
                        historyNavigateSwipeEnd: false,
                        inSlider: true,
                    });

                    expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                    expect(moveFramesContainerSpy).toHaveBeenCalledWith(-0);

                    const event = new Event('transitionend');
                    mockFramesContainerNode.dispatchEvent(event);

                    expect(setMultipageSpy).toHaveBeenCalledTimes(1);
                    expect(setMultipageSpy).toHaveBeenCalledWith({ currentIndex: 0 });
                    expect(setTitleSpy).toHaveBeenCalledTimes(0);
                    expect(changePageSpy).toHaveBeenCalledTimes(1);
                    expect(changePageSpy).toHaveBeenCalledWith({
                        url: 'https://yandex.ru/turbo?text=test_news1&some-more-data&new_overlay=1&check_swipe=1',
                        orig: 'https://original-host1.ru/article/1',
                        fromUrl: 'https://yandex.ru/turbo?text=test_news2&some-more-data&new_overlay=1&check_swipe=1',
                    }, 'https://yandex.ru/turbo?text=test_news1&some-more-data&new_overlay=1&check_swipe=1');

                    expect(controller.overlayIndex).toBe(0);
                });

                it('При плавном движении более, чем на половину страницы', () => {
                    controller.init(new Config({ urls, index: 1 }));
                    jest.clearAllMocks();

                    controller.handleSwipe({
                        state: ESwipeState.end,
                        value: 501,
                        smooth: true,
                        side: ESwipeSide.left,
                        historyNavigateSwipe: false,
                        historyNavigateSwipeEnd: false,
                        inSlider: true,
                    });

                    expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                    expect(moveFramesContainerSpy).toHaveBeenCalledWith(-0);

                    const event = new Event('transitionend');
                    mockFramesContainerNode.dispatchEvent(event);

                    expect(setMultipageSpy).toHaveBeenCalledTimes(1);
                    expect(setMultipageSpy).toHaveBeenCalledWith({ currentIndex: 0 });
                    expect(setTitleSpy).toHaveBeenCalledTimes(0);
                    expect(changePageSpy).toHaveBeenCalledTimes(1);
                    expect(changePageSpy).toHaveBeenCalledWith({
                        url: 'https://yandex.ru/turbo?text=test_news1&some-more-data&new_overlay=1&check_swipe=1',
                        orig: 'https://original-host1.ru/article/1',
                        fromUrl: 'https://yandex.ru/turbo?text=test_news2&some-more-data&new_overlay=1&check_swipe=1',
                    }, 'https://yandex.ru/turbo?text=test_news1&some-more-data&new_overlay=1&check_swipe=1');

                    expect(controller.overlayIndex).toBe(0);
                });

                it('При плавном движении менее, чем на половину страницы, не происходит', () => {
                    controller.init(new Config({ urls, index: 1 }));
                    jest.clearAllMocks();

                    controller.handleSwipe({
                        state: ESwipeState.end,
                        value: 500,
                        smooth: true,
                        side: ESwipeSide.left,
                        historyNavigateSwipe: false,
                        historyNavigateSwipeEnd: false,
                        inSlider: true,
                    });

                    expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                    expect(moveFramesContainerSpy).toHaveBeenCalledWith(-100);

                    expect(setMultipageSpy).toHaveBeenCalledTimes(0);
                    expect(setTitleSpy).toHaveBeenCalledTimes(0);
                    expect(changePageSpy).toHaveBeenCalledTimes(0);

                    expect(controller.overlayIndex).toBe(1);
                });

                it('При активной первой странице не происходит', () => {
                    controller.init(new Config({ urls, index: 0 }));
                    jest.clearAllMocks();

                    controller.handleSwipe({
                        state: ESwipeState.end,
                        value: 100,
                        smooth: false,
                        side: ESwipeSide.left,
                        historyNavigateSwipe: false,
                        historyNavigateSwipeEnd: false,
                        inSlider: true,
                    });

                    expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                    expect(moveFramesContainerSpy).toHaveBeenCalledWith(-0);

                    expect(setMultipageSpy).toHaveBeenCalledTimes(0);
                    expect(setTitleSpy).toHaveBeenCalledTimes(0);
                    expect(changePageSpy).toHaveBeenCalledTimes(0);

                    expect(controller.overlayIndex).toBe(0);
                });
            });

            it('Не меняет заголовок, если он был передан в конфиге', () => {
                controller.init(new Config({
                    urls,
                    index: 1,
                    title: 'test-title',
                }));
                jest.clearAllMocks();

                controller.handleSwipe({
                    state: ESwipeState.end,
                    value: -100,
                    smooth: false,
                    side: ESwipeSide.right,
                    historyNavigateSwipe: false,
                    historyNavigateSwipeEnd: false,
                    inSlider: true,
                });

                expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                expect(moveFramesContainerSpy).toHaveBeenCalledWith(-200);

                const event = new Event('transitionend');
                mockFramesContainerNode.dispatchEvent(event);

                expect(setMultipageSpy).toHaveBeenCalledTimes(0);
                expect(setTitleSpy).toHaveBeenCalledTimes(0);
                expect(changePageSpy).toHaveBeenCalledTimes(1);
                expect(changePageSpy).toHaveBeenCalledWith({
                    url: 'https://yandex.ru/turbo?text=test_news3&some-more-data&new_overlay=1&check_swipe=1',
                    orig: 'https://original-host3.ru/article/3',
                    fromUrl: 'https://yandex.ru/turbo?text=test_news2&some-more-data&new_overlay=1&check_swipe=1',
                }, 'https://yandex.ru/turbo?text=test_news3&some-more-data&new_overlay=1&check_swipe=1');

                expect(controller.overlayIndex).toBe(2);
            });
        });

        describe('Перелистывание страницы по вызову метода API', () => {
            let changePageSpy: jest.SpyInstance;

            beforeAll(() => {
                changePageSpy = jest.spyOn(navigator, 'changePage');
            });
            beforeEach(restoreDom);
            afterAll(() => {
                changePageSpy.mockRestore();
            });

            describe('Вправо', () => {
                it('При активной не последней странице', () => {
                    controller.init(new Config({ urls, index: 1 }));
                    jest.clearAllMocks();

                    controller.showNextPage();

                    expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                    expect(moveFramesContainerSpy).toHaveBeenCalledWith(-200);

                    const event = new Event('transitionend');
                    mockFramesContainerNode.dispatchEvent(event);

                    expect(setMultipageSpy).toHaveBeenCalledTimes(1);
                    expect(setMultipageSpy).toHaveBeenCalledWith({ currentIndex: 2 });
                    expect(setTitleSpy).toHaveBeenCalledTimes(0);
                    expect(changePageSpy).toHaveBeenCalledTimes(1);
                    expect(changePageSpy).toHaveBeenCalledWith({
                        url: 'https://yandex.ru/turbo?text=test_news3&some-more-data&new_overlay=1&check_swipe=1',
                        orig: 'https://original-host3.ru/article/3',
                        fromUrl: 'https://yandex.ru/turbo?text=test_news2&some-more-data&new_overlay=1&check_swipe=1',
                    }, 'https://yandex.ru/turbo?text=test_news3&some-more-data&new_overlay=1&check_swipe=1');

                    expect(controller.overlayIndex).toBe(2);
                });

                it('При активной последней странице не происходит', () => {
                    controller.init(new Config({ urls, index: 2 }));
                    jest.clearAllMocks();

                    controller.showNextPage();

                    expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                    expect(moveFramesContainerSpy).toHaveBeenCalledWith(-200);

                    expect(setMultipageSpy).toHaveBeenCalledTimes(0);
                    expect(setTitleSpy).toHaveBeenCalledTimes(0);
                    expect(changePageSpy).toHaveBeenCalledTimes(0);

                    expect(controller.overlayIndex).toBe(2);
                });
            });

            describe('Влево', () => {
                it('При активной не первой странице', () => {
                    controller.init(new Config({ urls, index: 1 }));
                    jest.clearAllMocks();

                    controller.showPrevPage();

                    expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                    expect(moveFramesContainerSpy).toHaveBeenCalledWith(-0);

                    const event = new Event('transitionend');
                    mockFramesContainerNode.dispatchEvent(event);

                    expect(setMultipageSpy).toHaveBeenCalledTimes(1);
                    expect(setMultipageSpy).toHaveBeenCalledWith({ currentIndex: 0 });
                    expect(setTitleSpy).toHaveBeenCalledTimes(0);
                    expect(changePageSpy).toHaveBeenCalledTimes(1);
                    expect(changePageSpy).toHaveBeenCalledWith({
                        url: 'https://yandex.ru/turbo?text=test_news1&some-more-data&new_overlay=1&check_swipe=1',
                        orig: 'https://original-host1.ru/article/1',
                        fromUrl: 'https://yandex.ru/turbo?text=test_news2&some-more-data&new_overlay=1&check_swipe=1',
                    }, 'https://yandex.ru/turbo?text=test_news1&some-more-data&new_overlay=1&check_swipe=1');

                    expect(controller.overlayIndex).toBe(0);
                });

                it('При активной первой странице не происходит', () => {
                    controller.init(new Config({ urls, index: 0 }));
                    jest.clearAllMocks();

                    controller.showPrevPage();

                    expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                    expect(moveFramesContainerSpy).toHaveBeenCalledWith(-0);

                    expect(setMultipageSpy).toHaveBeenCalledTimes(0);
                    expect(setTitleSpy).toHaveBeenCalledTimes(0);
                    expect(changePageSpy).toHaveBeenCalledTimes(0);

                    expect(controller.overlayIndex).toBe(0);
                });
            });

            it('Не меняет заголовок, если он был передан в конфиге', () => {
                controller.init(new Config({
                    urls,
                    index: 1,
                    title: 'test-title',
                }));
                jest.clearAllMocks();

                controller.showNextPage();

                expect(moveFramesContainerSpy).toHaveBeenCalledTimes(1);
                expect(moveFramesContainerSpy).toHaveBeenCalledWith(-200);

                const event = new Event('transitionend');
                mockFramesContainerNode.dispatchEvent(event);

                expect(setMultipageSpy).toHaveBeenCalledTimes(0);
                expect(setTitleSpy).toHaveBeenCalledTimes(0);
                expect(changePageSpy).toHaveBeenCalledTimes(1);
                expect(changePageSpy).toHaveBeenCalledWith({
                    url: 'https://yandex.ru/turbo?text=test_news3&some-more-data&new_overlay=1&check_swipe=1',
                    orig: 'https://original-host3.ru/article/3',
                    fromUrl: 'https://yandex.ru/turbo?text=test_news2&some-more-data&new_overlay=1&check_swipe=1',
                }, 'https://yandex.ru/turbo?text=test_news3&some-more-data&new_overlay=1&check_swipe=1');

                expect(controller.overlayIndex).toBe(2);
            });
        });

        describe('Предзагрузка страниц', () => {
            let appendFrameSpy: jest.SpyInstance;
            let showFrameNoActiveSpy: jest.SpyInstance;
            let removeFrameSpy: jest.SpyInstance;

            beforeAll(() => {
                appendFrameSpy = jest.spyOn(root, 'appendFrame');
                showFrameNoActiveSpy = jest.spyOn(root, 'showFrameNoActive');
                removeFrameSpy = jest.spyOn(root, 'removeFrame');
            });
            beforeEach(() => {
                restoreDom();
                jest.useFakeTimers();
            });
            afterAll(() => {
                appendFrameSpy.mockRestore();
                showFrameNoActiveSpy.mockRestore();
                removeFrameSpy.mockRestore();
                jest.useRealTimers();
            });

            it('Предзагружает следующие страницы', () => {
                controller.init(new Config({ urls, index: 0 }));

                expect(appendFrameSpy, 'Были добавлены фреймы до получения события show')
                    .toHaveBeenCalledTimes(0);
                expect(showFrameNoActiveSpy, 'Был вызов показа неактивного фрейма до получения события show')
                    .toHaveBeenCalledTimes(0);

                controller.handleShow({});

                expect(appendFrameSpy, 'Были добавлены фреймы сразу после события show без ожидания')
                    .toHaveBeenCalledTimes(0);
                expect(showFrameNoActiveSpy, 'Был вызов показа неактивного фрейма после события show без ожидания')
                    .toHaveBeenCalledTimes(0);

                jest.runAllTimers();

                expect(appendFrameSpy, 'Неверное количество вызовов метода добавления фрейма')
                    .toHaveBeenCalledTimes(1);
                expect(appendFrameSpy, 'Метод добавления фрейма был вызван с неверным параметром')
                    .toHaveBeenCalledWith('https://yandex.ru/turbo?text=test_news2' +
                        '&some-more-data&new_overlay=1&check_swipe=1');
                expect(showFrameNoActiveSpy, 'Не был вызан метод показа неактивного фрейма для соседней страницы')
                    .toHaveBeenCalledTimes(1);
                expect(appendFrameSpy, 'Метод показа неактивного фрейма был вызван с неверным параметром')
                    .toHaveBeenCalledWith('https://yandex.ru/turbo?text=test_news2' +
                        '&some-more-data&new_overlay=1&check_swipe=1');

                controller.handleShow({});
                jest.runAllTimers();

                expect(appendFrameSpy, 'Метод добавления фрейма был вызван лишний раз')
                    .toHaveBeenCalledTimes(2);
                expect(appendFrameSpy, 'Метод добавления фрейма был вызван с неверным параметром')
                    .toHaveBeenLastCalledWith('https://yandex.ru/turbo?text=test_news3' +
                        '&some-more-data&new_overlay=1&check_swipe=1');
                expect(showFrameNoActiveSpy, 'Метод показа неактивного фрейма был вызван для несоседней страницы')
                    .toHaveBeenCalledTimes(1);

                controller.handleShow({});
                jest.runAllTimers();

                expect(appendFrameSpy, 'Было предзагружено больше фреймов, чем нужно')
                    .toHaveBeenCalledTimes(2);
                expect(showFrameNoActiveSpy, 'Метод показа неактивного фрейма был вызван для несоседней страницы')
                    .toHaveBeenCalledTimes(1);
            });

            it('Выгружает страницы, далекие от текущей', () => {
                controller.init(new Config({ urls, index: 0 }));

                // Предзагружаем две следующие страницы
                controller.handleShow({});
                jest.runAllTimers();

                controller.handleShow({});
                jest.runAllTimers();

                expect(removeFrameSpy, 'Были вызовы удаления фреймов до перелистывания страниц')
                    .toHaveBeenCalledTimes(0);

                controller.showNextPage();

                const event = new Event('transitionend');
                mockFramesContainerNode.dispatchEvent(event);

                expect(removeFrameSpy, 'Фрейм был удален, хотя перелистываний было недостаточно')
                    .toHaveBeenCalledTimes(0);

                // После перелистывания на две страницы первая должна удалиться
                controller.showNextPage();

                mockFramesContainerNode.dispatchEvent(event);

                expect(removeFrameSpy, 'Неверное количество вызовов метода удаления фрейма')
                    .toHaveBeenCalledTimes(1);
                expect(removeFrameSpy, 'Судя по переданному параметру, был удален не тот фрейм, который следовало')
                    .toHaveBeenCalledWith('https://yandex.ru/turbo?text=test_news1' +
                        '&some-more-data&new_overlay=1&check_swipe=1');
            });
        });

        describe('Анимация горизонтального свайпа', () => {
            let disableAnimationSpy: jest.SpyInstance;
            let screenWidthSpy: jest.SpyInstance;

            beforeAll(() => {
                disableAnimationSpy = jest.spyOn(root, 'disableAnimation');
                screenWidthSpy = jest.spyOn(window.screen, 'width', 'get');
                screenWidthSpy.mockReturnValue(1000);
            });
            beforeEach(restoreDom);
            afterAll(() => {
                disableAnimationSpy.mockRestore();
            });

            it('Сдвигает контейнер фреймов во время горизонтального свайпа', () => {
                controller.init(new Config({ urls, index: 1 }));
                jest.clearAllMocks();

                controller.handleSwipe({
                    state: ESwipeState.horizontal,
                    value: -10,
                    historyNavigateSwipe: false,
                    historyNavigateSwipeEnd: false,
                    inSlider: true,
                });

                expect(disableAnimationSpy, 'Неверное количество вызовов метода отмены анимации фрейма')
                    .toHaveBeenCalledTimes(1);
                expect(moveFramesContainerSpy, 'Неверное количество вызовов метода сдвига контейнера с фреймами')
                    .toHaveBeenCalledTimes(1);
                expect(moveFramesContainerSpy, 'Контейнер с фреймами не сдвинулся на 10/1000 => 1% влево')
                    .toHaveBeenCalledWith(-101);

                jest.clearAllMocks();

                // Закончили свайп с плавным сдвигом на значение меньше половины экрана -
                // должны вернуться к исходному слайду
                controller.handleSwipe({
                    state: ESwipeState.end,
                    value: -10,
                    smooth: true,
                    side: ESwipeSide.right,
                    historyNavigateSwipe: false,
                    historyNavigateSwipeEnd: false,
                    inSlider: true,
                });

                expect(moveFramesContainerSpy, 'Неверное количество вызовов метода сдвига контейнера с фреймами')
                    .toHaveBeenCalledTimes(1);
                expect(moveFramesContainerSpy, 'Сдвиг контейнера с фреймами не вернулся к исходному значению')
                    .toHaveBeenCalledWith(-100);

                // Имитируем окончание анимации перелистывания
                const event = new Event('transitionend');
                mockFramesContainerNode.dispatchEvent(event);

                jest.clearAllMocks();

                // Теперь двигаем немного вправо
                controller.handleSwipe({
                    state: ESwipeState.horizontal,
                    value: 10,
                    historyNavigateSwipe: false,
                    historyNavigateSwipeEnd: false,
                    inSlider: true,
                });

                expect(disableAnimationSpy, 'Неверное количество вызовов метода отмены анимации фрейма')
                    .toHaveBeenCalledTimes(1);
                expect(moveFramesContainerSpy, 'Неверное количество вызовов метода сдвига контейнера с фреймами')
                    .toHaveBeenCalledTimes(1);
                expect(moveFramesContainerSpy, 'Контейнер с фреймами не сдвинулся на 10/1000 => 1% вправо')
                    .toHaveBeenCalledWith(-99);
            });

            it('Уменьшает сдвиг при горизонтальном свайпе за границу оверлея', () => {
                controller.init(new Config({ urls, index: 0 }));
                jest.clearAllMocks();

                controller.handleSwipe({
                    state: ESwipeState.horizontal,
                    value: 500,
                    historyNavigateSwipe: false,
                    historyNavigateSwipeEnd: false,
                    inSlider: true,
                });

                expect(disableAnimationSpy, 'Неверное количество вызовов метода отмены анимации фрейма')
                    .toHaveBeenCalledTimes(1);
                expect(moveFramesContainerSpy, 'Неверное количество вызовов метода сдвига контейнера с фреймами')
                    .toHaveBeenCalledTimes(1);
                expect(moveFramesContainerSpy, 'Контейнер с фреймами не сдвинулся на (500/5/1000 => 10%) вправо')
                    .toHaveBeenCalledWith(10);
            });

            it('После свайпа за край оверлея работает свайп в другую сторону', () => {
                controller.init(new Config({ urls, index: 0 }));
                jest.clearAllMocks();

                // Свайпаем за край
                controller.handleSwipe({
                    state: ESwipeState.horizontal,
                    value: 500,
                    historyNavigateSwipe: false,
                    historyNavigateSwipeEnd: false,
                    inSlider: true,
                });

                // Сообщаем, что свайп закончен
                controller.handleSwipe({
                    state: ESwipeState.end,
                    value: 500,
                    smooth: false,
                    side: ESwipeSide.right,
                    historyNavigateSwipe: false,
                    historyNavigateSwipeEnd: false,
                    inSlider: true,
                });

                // Имитируем окончание анимации
                const event = new Event('transitionend');
                mockFramesContainerNode.dispatchEvent(event);

                jest.clearAllMocks();

                // Свайпаем в другую сторону
                controller.handleSwipe({
                    state: ESwipeState.horizontal,
                    value: -10,
                    historyNavigateSwipe: false,
                    historyNavigateSwipeEnd: false,
                    inSlider: true,
                });

                expect(disableAnimationSpy, 'Неверное количество вызовов метода отмены анимации фрейма')
                    .toHaveBeenCalledTimes(1);
                expect(moveFramesContainerSpy, 'Неверное количество вызовов метода сдвига контейнера с фреймами')
                    .toHaveBeenCalledTimes(1);
            });
        });

        describe('Открытие и закрытие страниц', () => {
            it('После скролла до следующей страницы в бесконечной ленте закрывает оверлей без ошибок', () => {
                // Перед инитом контроллера главный модуль Overlay добавляет фрейм с открываемой страницей
                root.appendFrame(urls[0].frameUrl);

                const emitErrorSpy = jest.spyOn(emitErrorUtil, 'emitError');

                controller.init(new Config({ urls }));

                controller.handleActiveDocumentChanged({
                    url: 'https://yandex.ru/turbo?text=new_page',
                    orig: 'https://original-host1.ru/article/1',
                    cleanUrl: 'https://yandex.ru/turbo?text=new_page',
                    title: 'Заголовок',
                });

                expect(emitErrorSpy, 'Появились ошибки до закрытия мультислайдера')
                    .toHaveBeenCalledTimes(0);

                controller.reset();

                expect(emitErrorSpy, 'Появились ошибки после закрытия мультислайдера')
                    .toHaveBeenCalledTimes(0);

                controller.init(new Config({ urls }));

                expect(emitErrorSpy, 'Появились ошибки после повторного открытия мультислайдера')
                    .toHaveBeenCalledTimes(0);

                emitErrorSpy.mockRestore();
            });
        });
    });
});
