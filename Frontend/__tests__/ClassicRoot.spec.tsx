import {
    renderHead,
    renderBackButton,
    renderCloseButton,
    renderTitle,
    renderDots,
    renderCentralContainer,
} from '../renderHead';
import * as baseCls from '../../Base/Classes';
import * as cls from '../Classes';
import * as rafUtil from '../../../../utils/requestAnimationFrame';

import { restoreDom, clearNamespace } from '../../../../__tests__/tests-lib';

import { ClassicRoot } from '../ClassicRoot';

const mockShow = jest.fn();
const mockShowNoActive = jest.fn();
const mockHide = jest.fn();
const mockMove = jest.fn();
const mockRemove = jest.fn();
const mockGetOffset = jest.fn();
const mockFrame = jest.fn().mockImplementation(() => {
    return {
        show: mockShow,
        showNoActive: mockShowNoActive,
        hide: mockHide,
        move: mockMove,
        remove: mockRemove,
        getOffset: mockGetOffset,
    };
});
jest.mock('../../../Frame/Frame', () => ({
    __esModule: true,
    Frame: (rootNode: HTMLElement, url: string) => mockFrame(rootNode, url),
}));

describe('Турбо-оверлей', () => {
    describe('Модуль создания html (ClassicView)', () => {
        beforeAll(() => {
            jest.spyOn(rafUtil, 'overlayRequestAnimationFrame').mockImplementation(cb => {
                cb(0);
                return 0;
            });
        });
        beforeEach(restoreDom);
        afterEach(() => {
            restoreDom();
            jest.clearAllMocks();
        });
        afterAll(() => {
            clearNamespace();
            jest.restoreAllMocks();
        });

        it('Cоздает корректную шапку', () => {
            const centralContainer = renderCentralContainer(renderTitle(), renderDots());
            expect(renderHead(renderBackButton(), renderCloseButton(), centralContainer)).toMatchSnapshot();
        });

        it('Создает корректный html при иницилизации', () => {
            const classicRoot = new ClassicRoot();
            expect(document.documentElement).toMatchSnapshot();

            expect(classicRoot.getRootNode(), 'Возвращает некорректный элемент в качестве рута')
                .toEqual(document.querySelector(`.${baseCls.block}`));
        });

        it('Показывает и прячет спиннер', () => {
            const classicRoot = new ClassicRoot();
            expect(document.querySelector(`.${cls.spinnerVisible}`), 'Спиннер добавлен изначально')
                .toBeNull();

            classicRoot.showSpinner();
            expect(document.querySelector(`.${cls.spinnerVisible}`), 'Спиннер не добавился')
                .not.toBeNull();

            classicRoot.hideSpinner();
            expect(document.querySelector(`.${cls.spinnerVisible}`), 'Спиннер не удалился')
                .toBeNull();

            expect(document.documentElement).toMatchSnapshot();
        });

        it('Показывает и прячет шапку', () => {
            const classicRoot = new ClassicRoot();
            expect(document.querySelector(`.${cls.overlayHeadVisible}`), 'Шапка не показана изначально')
                .not.toBeNull();
            expect(document.querySelector(`.${cls.blockHeaderHidden}`), 'Оверлей имеет класс спрятанной шапки')
                .toBeNull();

            classicRoot.hideHead();
            expect(document.querySelector(`.${cls.overlayHeadVisible}`), 'Шапка не спряталась')
                .toBeNull();
            expect(document.querySelector(`.${cls.blockHeaderHidden}`), 'Оверлей не имеет класса спрятанной шапки')
                .not.toBeNull();

            classicRoot.showHead();
            expect(document.querySelector(`.${cls.overlayHeadVisible}`), 'Шапка не показалась')
                .not.toBeNull();
            expect(document.querySelector(`.${cls.blockHeaderHidden}`), 'Оверлей имеет класс спрятанной шапки после показа')
                .toBeNull();

            expect(document.documentElement).toMatchSnapshot();
        });

        it('Показывает и прячет крестик', () => {
            const classicRoot = new ClassicRoot();
            expect(document.querySelector(`.${cls.closeButtonVisible}`), 'Крестик показан изначально').toBeNull();
            expect(document.documentElement).toMatchSnapshot();

            classicRoot.showCloseButtonElement();
            expect(document.querySelector(`.${cls.closeButtonVisible}`), 'Крестик не показался').not.toBeNull();
            expect(document.documentElement).toMatchSnapshot();

            classicRoot.hideCloseButtonElement();
            expect(document.querySelector(`.${cls.closeButtonVisible}`), 'Крестик не скрылся').toBeNull();

            expect(document.documentElement).toMatchSnapshot();
        });

        it('Показывает и прячет оверлей', () => {
            const classicRoot = new ClassicRoot();
            expect(document.querySelector(`.${baseCls.blockVisible}`), 'Оверлей показан изначально')
                .toBeNull();

            classicRoot.showOverlay();
            expect(document.querySelector(`.${baseCls.blockVisible}`), 'Оверлей не показался')
                .not.toBeNull();

            classicRoot.hideOverlay();
            expect(document.querySelector(`.${baseCls.blockVisible}`), 'Оверлей не спрятался')
                .toBeNull();

            expect(document.documentElement).toMatchSnapshot();
        });

        it('Добавляет, показывает, прячет и удаляет iframe', () => {
            const classicRoot = new ClassicRoot();

            expect(mockFrame, 'Есть созданные фреймы').toHaveBeenCalledTimes(0);
            expect(classicRoot.getFrame('https://example.com'), 'Фрейм изначально создан').toBeUndefined();
            expect(mockShow, 'Метод показа фрейма был вызван изначально').toHaveBeenCalledTimes(0);
            expect(mockShowNoActive, 'Метод показа неактивного фрейма был вызван изначально')
                .toHaveBeenCalledTimes(0);
            expect(mockHide, 'Метод скрытия фрейма был вызван изначально').toHaveBeenCalledTimes(0);
            expect(mockRemove, 'Метод удаления фрейма был вызван изначально').toHaveBeenCalledTimes(0);

            // продублировано для проверки работы кэша
            classicRoot.appendFrame('https://example.com');
            classicRoot.appendFrame('https://example.com');

            expect(mockFrame, 'Неверное количество вызовов конструктора фрейма').toHaveBeenCalledTimes(1);
            expect(classicRoot.getFrame('https://example.com'), 'Отсутствует фрейм').not.toBeUndefined();
            expect(mockShow, 'Неверное количество вызовов метода показа фрейма').toHaveBeenCalledTimes(0);
            expect(mockShowNoActive, 'Неверное количество вызовов метода показа неактивного фрейма')
                .toHaveBeenCalledTimes(0);
            expect(mockHide, 'Неверное количество вызовов метода скрытия фрейма').toHaveBeenCalledTimes(0);
            expect(mockRemove, 'Неверное количество вызовов метода удаления фрейма').toHaveBeenCalledTimes(0);

            classicRoot.showFrame('https://example.com');

            expect(mockFrame, 'Неверное количество вызовов конструктора фрейма').toHaveBeenCalledTimes(1);
            expect(classicRoot.getFrame('https://example.com'), 'Отсутствует фрейм').not.toBeUndefined();
            expect(mockShow, 'Неверное количество вызовов метода показа фрейма').toHaveBeenCalledTimes(1);
            expect(mockShowNoActive, 'Неверное количество вызовов метода показа неактивного фрейма')
                .toHaveBeenCalledTimes(0);
            expect(mockHide, 'Неверное количество вызовов метода скрытия фрейма').toHaveBeenCalledTimes(0);
            expect(mockRemove, 'Неверное количество вызовов метода удаления фрейма').toHaveBeenCalledTimes(0);

            classicRoot.showFrameNoActive('https://example.com');

            expect(mockFrame, 'Неверное количество вызовов конструктора фрейма').toHaveBeenCalledTimes(1);
            expect(classicRoot.getFrame('https://example.com'), 'Отсутствует фрейм').not.toBeUndefined();
            expect(mockShow, 'Неверное количество вызовов метода показа фрейма').toHaveBeenCalledTimes(1);
            expect(mockShowNoActive, 'Неверное количество вызовов метода показа неактивного фрейма')
                .toHaveBeenCalledTimes(1);
            expect(mockHide, 'Неверное количество вызовов метода скрытия фрейма').toHaveBeenCalledTimes(0);
            expect(mockRemove, 'Неверное количество вызовов метода удаления фрейма').toHaveBeenCalledTimes(0);

            classicRoot.hideFrame('https://example.com');

            expect(mockFrame, 'Неверное количество вызовов конструктора фрейма').toHaveBeenCalledTimes(1);
            expect(classicRoot.getFrame('https://example.com'), 'Отсутствует фрейм').not.toBeUndefined();
            expect(mockShow, 'Неверное количество вызовов метода показа фрейма').toHaveBeenCalledTimes(1);
            expect(mockShowNoActive, 'Неверное количество вызовов метода показа неактивного фрейма')
                .toHaveBeenCalledTimes(1);
            expect(mockHide, 'Неверное количество вызовов метода скрытия фрейма').toHaveBeenCalledTimes(1);
            expect(mockRemove, 'Неверное количество вызовов метода удаления фрейма').toHaveBeenCalledTimes(0);

            classicRoot.removeFrame('https://example.com');

            expect(mockFrame, 'Неверное количество вызовов конструктора фрейма').toHaveBeenCalledTimes(1);
            expect(classicRoot.getFrame('https://example.com'), 'Фрейм не удалился').toBeUndefined();
            expect(mockShow, 'Неверное количество вызовов метода показа фрейма').toHaveBeenCalledTimes(1);
            expect(mockShowNoActive, 'Неверное количество вызовов метода показа неактивного фрейма')
                .toHaveBeenCalledTimes(1);
            expect(mockHide, 'Неверное количество вызовов метода скрытия фрейма').toHaveBeenCalledTimes(1);
            expect(mockRemove, 'Неверное количество вызовов метода удаления фрейма').toHaveBeenCalledTimes(1);
        });

        it('Добавляет фрейм и возвращает его в результате', () => {
            const classicRoot = new ClassicRoot();

            const appendedFrame = classicRoot.appendFrame('https://example.com');

            const gotFrame = classicRoot.getFrame('https://example.com');

            expect(appendedFrame, 'Возвращенный фрейм не соответствует добавленному').toEqual(gotFrame);
        });

        it('Добавляет и удаляет несколько iframe', () => {
            const classicRoot = new ClassicRoot();

            expect(mockFrame, 'Есть созданные фреймы').toHaveBeenCalledTimes(0);
            expect(classicRoot.getFrame('https://example.com'), 'Изначально создан фрейм для первой ссылки')
                .toBeUndefined();
            expect(classicRoot.getFrame('https://example.com/1'), 'Изначально создан фрейм для второй ссылки')
                .toBeUndefined();
            expect(mockRemove, 'Метод удаления фрейма был вызван изначально').toHaveBeenCalledTimes(0);

            classicRoot.appendFrame('https://example.com');

            expect(mockFrame, 'Неверное количество вызовов конструктора фрейма').toHaveBeenCalledTimes(1);
            expect(classicRoot.getFrame('https://example.com'), 'Отсутствует фрейм для первой ссылки')
                .not.toBeUndefined();
            expect(classicRoot.getFrame('https://example.com/1'), 'Фрейм для второй ссылки также добавился')
                .toBeUndefined();
            expect(mockRemove, 'Неверное количество вызовов метода удаления фрейма').toHaveBeenCalledTimes(0);

            classicRoot.appendFrame('https://example.com/1');

            expect(mockFrame, 'Неверное количество вызовов конструктора фрейма').toHaveBeenCalledTimes(2);
            expect(classicRoot.getFrame('https://example.com'), 'Отсутствует фрейм для первой ссылки')
                .not.toBeUndefined();
            expect(classicRoot.getFrame('https://example.com/1'), 'Отсутствует фрейм для второй ссылки')
                .not.toBeUndefined();
            expect(mockRemove, 'Неверное количество вызовов метода удаления фрейма').toHaveBeenCalledTimes(0);

            classicRoot.removeFrame('https://example.com');

            expect(mockFrame, 'Неверное количество вызовов конструктора фрейма').toHaveBeenCalledTimes(2);
            expect(classicRoot.getFrame('https://example.com'), 'Фрейм для первой ссылки не был удален')
                .toBeUndefined();
            expect(classicRoot.getFrame('https://example.com/1'), 'Фрейм для второй ссылки неожиданно удалился')
                .not.toBeUndefined();
            expect(mockRemove, 'Неверное количество вызовов метода удаления фрейма').toHaveBeenCalledTimes(1);

            classicRoot.removeFrame('https://example.com/1');

            expect(mockFrame, 'Неверное количество вызовов конструктора фрейма').toHaveBeenCalledTimes(2);
            expect(classicRoot.getFrame('https://example.com'), 'Фрейм для первой ссылки не был удален')
                .toBeUndefined();
            expect(classicRoot.getFrame('https://example.com/1'), 'Фрейм для второй ссылки не был удален')
                .toBeUndefined();
            expect(mockRemove, 'Неверное количество вызовов метода удаления фрейма').toHaveBeenCalledTimes(2);
        });

        it('Подменяет iframe для позиции кэша', () => {
            const classicRoot = new ClassicRoot();

            expect(mockFrame, 'Есть созданные фреймы').toHaveBeenCalledTimes(0);
            expect(classicRoot.getFrame('https://example.com'), 'Фрейм изначально создан').toBeUndefined();
            expect(mockShow, 'Метод показа фрейма был вызван изначально').toHaveBeenCalledTimes(0);
            expect(mockShowNoActive, 'Неверное количество вызовов метода показа неактивного фрейма')
                .toHaveBeenCalledTimes(0);
            expect(mockHide, 'Метод скрытия фрейма был вызван изначально').toHaveBeenCalledTimes(0);
            expect(mockRemove, 'Метод удаления фрейма был вызван изначально').toHaveBeenCalledTimes(0);

            classicRoot.appendFrame('https://example.com');

            expect(mockFrame, 'Неверное количество вызовов конструктора фрейма').toHaveBeenCalledTimes(1);
            expect(classicRoot.getFrame('https://example.com'), 'Отсутствует фрейм').not.toBeUndefined();
            expect(mockShow, 'Неверное количество вызовов метода показа фрейма').toHaveBeenCalledTimes(0);
            expect(mockShowNoActive, 'Неверное количество вызовов метода показа неактивного фрейма')
                .toHaveBeenCalledTimes(0);
            expect(mockHide, 'Неверное количество вызовов метода скрытия фрейма').toHaveBeenCalledTimes(0);
            expect(mockRemove, 'Неверное количество вызовов метода удаления фрейма').toHaveBeenCalledTimes(0);

            classicRoot.recreateFrame('https://example.com', 'some-string');

            expect(mockFrame, 'Неверное количество вызовов конструктора фрейма').toHaveBeenCalledTimes(2);
            expect(mockFrame.mock.calls[1][1], 'Второй фрейм был создан с неверным урлом').toBe('some-string');
            expect(classicRoot.getFrame('https://example.com'), 'Отсутствует фрейм').not.toBeUndefined();
            expect(mockShow, 'Неверное количество вызовов метода показа фрейма').toHaveBeenCalledTimes(0);
            expect(mockShowNoActive, 'Неверное количество вызовов метода показа неактивного фрейма')
                .toHaveBeenCalledTimes(0);
            expect(mockHide, 'Неверное количество вызовов метода скрытия фрейма').toHaveBeenCalledTimes(0);
            expect(mockRemove, 'Неверное количество вызовов метода удаления фрейма').toHaveBeenCalledTimes(1);

            classicRoot.showFrame('https://example.com');

            expect(mockFrame, 'Неверное количество вызовов конструктора фрейма').toHaveBeenCalledTimes(2);
            expect(classicRoot.getFrame('https://example.com'), 'Отсутствует фрейм').not.toBeUndefined();
            expect(mockShow, 'Неверное количество вызовов метода показа фрейма').toHaveBeenCalledTimes(1);
            expect(mockShowNoActive, 'Неверное количество вызовов метода показа неактивного фрейма')
                .toHaveBeenCalledTimes(0);
            expect(mockHide, 'Неверное количество вызовов метода скрытия фрейма').toHaveBeenCalledTimes(0);
            expect(mockRemove, 'Неверное количество вызовов метода удаления фрейма').toHaveBeenCalledTimes(1);

            classicRoot.showFrameNoActive('https://example.com');

            expect(mockFrame, 'Неверное количество вызовов конструктора фрейма').toHaveBeenCalledTimes(2);
            expect(classicRoot.getFrame('https://example.com'), 'Отсутствует фрейм').not.toBeUndefined();
            expect(mockShow, 'Неверное количество вызовов метода показа фрейма').toHaveBeenCalledTimes(1);
            expect(mockShowNoActive, 'Неверное количество вызовов метода показа неактивного фрейма')
                .toHaveBeenCalledTimes(1);
            expect(mockHide, 'Неверное количество вызовов метода скрытия фрейма').toHaveBeenCalledTimes(0);
            expect(mockRemove, 'Неверное количество вызовов метода удаления фрейма').toHaveBeenCalledTimes(1);

            classicRoot.hideFrame('https://example.com');

            expect(mockFrame, 'Неверное количество вызовов конструктора фрейма').toHaveBeenCalledTimes(2);
            expect(classicRoot.getFrame('https://example.com'), 'Отсутствует фрейм').not.toBeUndefined();
            expect(mockShow, 'Неверное количество вызовов метода показа фрейма').toHaveBeenCalledTimes(1);
            expect(mockShowNoActive, 'Неверное количество вызовов метода показа неактивного фрейма')
                .toHaveBeenCalledTimes(1);
            expect(mockHide, 'Неверное количество вызовов метода скрытия фрейма').toHaveBeenCalledTimes(1);
            expect(mockRemove, 'Неверное количество вызовов метода удаления фрейма').toHaveBeenCalledTimes(1);

            classicRoot.removeFrame('https://example.com');

            expect(mockFrame, 'Неверное количество вызовов конструктора фрейма').toHaveBeenCalledTimes(2);
            expect(classicRoot.getFrame('https://example.com'), 'Фрейм не удалился').toBeUndefined();
            expect(mockShow, 'Неверное количество вызовов метода показа фрейма').toHaveBeenCalledTimes(1);
            expect(mockShowNoActive, 'Неверное количество вызовов метода показа неактивного фрейма')
                .toHaveBeenCalledTimes(1);
            expect(mockHide, 'Неверное количество вызовов метода скрытия фрейма').toHaveBeenCalledTimes(1);
            expect(mockRemove, 'Неверное количество вызовов метода удаления фрейма').toHaveBeenCalledTimes(2);
        });

        it('Меняет режим singlepage-multipage', () => {
            const classicRoot = new ClassicRoot();

            classicRoot.setMultipage({ pagesCount: 5, currentIndex: 4 });

            expect(document.documentElement).toMatchSnapshot();

            classicRoot.setSinglepage();

            expect(document.documentElement).toMatchSnapshot();
        });

        it('Сдвигает контейнер с фреймами', () => {
            const classicRoot = new ClassicRoot();

            expect(classicRoot.getFramesContainerNode().style.transform, 'Стиль контейнера с фреймами установлен исходно')
                .toEqual('');
            expect(classicRoot.getFramesContainerNode()).toMatchSnapshot();

            classicRoot.moveFramesContainer(10);

            expect(classicRoot.getFramesContainerNode().style.transform, 'Не был изменен стиль контейнера с фреймами')
                .toEqual('translate3d(10%, 0, 0)');
            expect(classicRoot.getFramesContainerNode()).toMatchSnapshot();

            classicRoot.moveFramesContainer(-200);

            expect(classicRoot.getFramesContainerNode().style.transform, 'Не был изменен стиль контейнера с фреймами')
                .toEqual('translate3d(-200%, 0, 0)');
            expect(classicRoot.getFramesContainerNode()).toMatchSnapshot();
        });

        it('Отключает и включает анимацию на контейнере с фреймами', () => {
            const classicRoot = new ClassicRoot();

            expect(
                classicRoot.getFramesContainerNode().classList.contains(cls.framesContainerNoAnimation),
                'Изначально у контейнера с фреймами отключена анимация'
            ).toEqual(false);

            classicRoot.disableAnimation();

            expect(
                classicRoot.getFramesContainerNode().classList.contains(cls.framesContainerNoAnimation),
                'Анимация контейнера с фреймами не была отключена'
            ).toEqual(true);

            classicRoot.enableAnimation();

            expect(
                classicRoot.getFramesContainerNode().classList.contains(cls.framesContainerNoAnimation),
                'Анимация контейнера с фреймами не была включена'
            ).toEqual(false);
        });

        it('Стреляет ошибки на непонятное поведение', () => {
            const classicRoot = new ClassicRoot();

            let errorFn = jest.fn();
            window.onerror = errorFn;
            classicRoot.removeFrame('https://yandex.ru/turbo?text=some-string');

            const getErr = (msg: string) => [
                msg,
                'https://yandex.ru/',
                0,
                0,
                new Error(msg),
            ];

            expect(errorFn.mock.calls.length).toBe(1);
            expect(errorFn.mock.calls[0]).toEqual(getErr('TURBO-OVERLAY: trying to remove non existant frame'));

            errorFn = jest.fn();
            window.onerror = errorFn;
            classicRoot.hideFrame('https://yandex.ru/turbo?text=some-string');

            expect(errorFn.mock.calls.length).toBe(1);
            expect(errorFn.mock.calls[0]).toEqual(getErr('TURBO-OVERLAY: trying to hide non existant frame'));

            errorFn = jest.fn();
            window.onerror = errorFn;
            classicRoot.showFrame('https://yandex.ru/turbo?text=some-string');

            expect(errorFn.mock.calls.length).toBe(1);
            expect(errorFn.mock.calls[0]).toEqual(getErr('TURBO-OVERLAY: trying to show non existant frame'));

            errorFn = jest.fn();
            window.onerror = errorFn;
            classicRoot.showFrameNoActive('https://yandex.ru/turbo?text=some-string');

            expect(errorFn.mock.calls.length).toBe(1);
            expect(errorFn.mock.calls[0])
                .toEqual(getErr('TURBO-OVERLAY: trying to show no active non existant frame'));

            errorFn = jest.fn();
            window.onerror = errorFn;
            classicRoot.recreateFrame('https://yandex.ru/turbo?text=some-string', 'https://yandex.ru/turbo?text=some-other-string');

            expect(errorFn.mock.calls.length).toBe(1);
            expect(errorFn.mock.calls[0]).toEqual(getErr('TURBO-OVERLAY: trying to recreate non existant frame'));
        });
    });
});
