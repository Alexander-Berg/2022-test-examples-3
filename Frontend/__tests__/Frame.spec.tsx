import { IRoot } from '../../interfaces';
import { createRoots } from '../../createRoots';
import { Frame } from '../Frame';
import { Config } from '../../../utils/config';
import { renderContainer, renderFrame } from '../render';
import { restoreDom } from '../../../__tests__/tests-lib';
import { createHtmlElement } from '../../../utils/createElement';
import * as cls from '../Classes';

const ACTIVATE_TIMESTAMP = 12345678910123;

describe('Турбо-оверлей', () => {
    describe('Frame', () => {
        beforeEach(restoreDom);
        afterEach(restoreDom);

        Date.now = jest.fn(() => ACTIVATE_TIMESTAMP);

        let root: IRoot;
        beforeEach(function() {
            const url = 'https://example.com';
            const config = new Config({ urls: [{
                frameUrl: url,
                displayUrl: url,
                originalUrl: url,
            }] }, null, {
                type: 'click',
                timestamp: Date.now(),
            });

            root = createRoots().singlepage;
            root.setConfig(config);

            restoreDom();
        });

        it('Cоздает корректную разметку', () => {
            const frame1 = renderFrame('https://yandex.ru/turbo?text=about');
            expect(frame1).toMatchSnapshot();
            expect(renderContainer(frame1)).toMatchSnapshot();

            const frame2 = renderFrame('https://yandex.ru/turbo?text=test_news');
            expect(frame2).toMatchSnapshot();
            expect(renderContainer(frame2)).toMatchSnapshot();
        });

        it('Добавляет, показывает, прячет и удаляет iframe', () => {
            const framesContainerNode = createHtmlElement('div');
            document.body.appendChild(framesContainerNode);

            expect(document.querySelector(`.${cls.frame}`), 'Изначально есть какие-то iframe')
                .toBeNull();

            const exampleFrame = new Frame(framesContainerNode, 'https://example.com', root);

            expect(document.querySelector(`.${cls.frame}`), 'Iframe не добавился')
                .not.toBeNull();
            expect(document.querySelectorAll(`.${cls.frame}`).length, 'Добавилось больше одного iframe')
                .toBe(1);
            expect(document.querySelector(`.${cls.frameBodyVisible}`), 'Iframe показан изначально')
                .toBeNull();
            expect(document.querySelector(`.${cls.frame}`).getAttribute('src'))
                .toBe('https://example.com');
            expect(document.documentElement).toMatchSnapshot();

            exampleFrame.showNoActive();

            expect(document.querySelector(`.${cls.frameBodyVisible}`), 'Iframe не показался')
                .not.toBeNull();
            expect(document.documentElement).toMatchSnapshot();

            exampleFrame.show();

            expect(document.querySelector(`.${cls.frameBodyVisible}`), 'Iframe не показался')
                .not.toBeNull();
            expect(document.documentElement).toMatchSnapshot();

            exampleFrame.hide();

            expect(document.querySelector(`.${cls.frameBodyVisible}`), 'Iframe не спрятался')
                .toBeNull();
            expect(document.documentElement).toMatchSnapshot();

            exampleFrame.show();

            expect(document.querySelector(`.${cls.frameBodyVisible}`), 'Iframe не показался')
                .not.toBeNull();
            expect(document.documentElement).toMatchSnapshot();

            exampleFrame.showNoActive();

            expect(document.querySelector(`.${cls.frameBodyVisible}`), 'Iframe не показался')
                .not.toBeNull();
            expect(document.documentElement).toMatchSnapshot();

            exampleFrame.hide();

            expect(document.querySelector(`.${cls.frameBodyVisible}`), 'Iframe не спрятался')
                .toBeNull();
            expect(document.documentElement).toMatchSnapshot();

            exampleFrame.remove();

            expect(document.querySelector(`.${cls.frame}`), 'Iframe не удалился')
                .toBeNull();
            expect(document.documentElement).toMatchSnapshot();
        });

        it('Отправляет сообщения в iframe', () => {
            const framesContainerNode = createHtmlElement('div');
            document.body.appendChild(framesContainerNode);

            const exampleFrame = new Frame(framesContainerNode, 'https://example.com', root);

            const defaultEventSource = {} as MessageEventSource;

            // Помечаем фрейм загрузившимся
            Frame.handleShow({
                title: 'Турбо-страницы',
                cleanUrl: 'https://example.com',
                fixSwipe: false,
                url: 'https://example.com',
                originalUrl: 'https://test_news.com',
                source: exampleFrame.getElement().contentWindow || defaultEventSource,
                action: 'show',
            }, exampleFrame.getElement().contentWindow || defaultEventSource);

            const postMessageSpy = jest.spyOn(exampleFrame.getElement().contentWindow, 'postMessage');

            exampleFrame.showNoActive();

            expect(postMessageSpy, 'Во фрейм было отправлено сообщение').toHaveBeenCalledTimes(0);

            exampleFrame.show();

            expect(postMessageSpy, 'Во фрейм не было отправлено сообщение активации').toHaveBeenCalledTimes(1);
            expect(postMessageSpy, 'Было отправлено неправильное сообщение во фрейм')
                .toHaveBeenCalledWith(JSON.stringify({ action: 'overlay-slider-visible', type: 'click', timestamp: ACTIVATE_TIMESTAMP }), '*');

            postMessageSpy.mockClear();

            exampleFrame.hide();

            expect(postMessageSpy, 'Во фрейм не было отправлено сообщение деактивации').toHaveBeenCalledTimes(1);
            expect(postMessageSpy, 'Было отправлено неправильное сообщение во фрейм')
                .toHaveBeenCalledWith(JSON.stringify({ action: 'overlay-slider-hidden' }), '*');

            postMessageSpy.mockClear();
            exampleFrame.show();

            expect(postMessageSpy, 'Во фрейм не было отправлено сообщение активации').toHaveBeenCalledTimes(1);
            expect(postMessageSpy, 'Было отправлено неправильное сообщение во фрейм')
                .toHaveBeenCalledWith(JSON.stringify({ action: 'overlay-slider-visible', type: 'other', timestamp: 0 }), '*');

            postMessageSpy.mockClear();

            exampleFrame.showNoActive();

            expect(postMessageSpy, 'Во фрейм не было отправлено сообщение деактивации').toHaveBeenCalledTimes(1);
            expect(postMessageSpy, 'Было отправлено неправильное сообщение во фрейм')
                .toHaveBeenCalledWith(JSON.stringify({ action: 'overlay-slider-hidden' }), '*');

            postMessageSpy.mockClear();

            exampleFrame.hide();

            expect(postMessageSpy, 'Во фрейм было лишний раз отправлено сообщение').toHaveBeenCalledTimes(0);
        });

        it('Добавляет, показывает и прячет несколько iframe', () => {
            const framesContainerNode = createHtmlElement('div');
            document.body.appendChild(framesContainerNode);

            expect(document.querySelector(`.${cls.frame}`), 'Изначально есть какие-то iframe')
                .toBeNull();

            const exampleFrame = new Frame(framesContainerNode, 'https://example.com', root);
            const example1Frame = new Frame(framesContainerNode, 'https://example1.com', root);

            expect(document.querySelector(`.${cls.frame}`), 'Iframe не добавился')
                .not.toBeNull();
            expect(document.querySelectorAll(`.${cls.frame}`).length, 'Добавилось больше одного iframe')
                .toBe(2);
            expect(document.querySelector(`.${cls.frameBodyVisible}`), 'Iframe показан изначально')
                .toBeNull();
            expect(document.documentElement).toMatchSnapshot();

            exampleFrame.show();

            expect(document.querySelector(`.${cls.frameBodyVisible}`), 'Iframe не показался')
                .not.toBeNull();
            expect(document.querySelectorAll(`.${cls.frameBodyVisible}`).length, 'Показалось больше одного iframe')
                .toBe(1);
            expect(document.documentElement).toMatchSnapshot();

            exampleFrame.hide();

            expect(document.querySelector(`.${cls.frameBodyVisible}`), 'Iframe не скрылся')
                .toBeNull();
            expect(document.documentElement).toMatchSnapshot();

            example1Frame.show();

            expect(document.querySelector(`.${cls.frameBodyVisible}`), 'Iframe не показался')
                .not.toBeNull();
            expect(document.querySelectorAll(`.${cls.frameBodyVisible}`).length, 'Показалось больше одного iframe')
                .toBe(1);
            expect(document.documentElement).toMatchSnapshot();

            example1Frame.hide();

            expect(document.querySelector(`.${cls.frameBodyVisible}`), 'Iframe не скрылся')
                .toBeNull();
            expect(document.documentElement).toMatchSnapshot();

            exampleFrame.remove();

            expect(document.querySelectorAll(`.${cls.frame}`).length, 'Первый Iframe не удалился')
                .toBe(1);
            expect(document.documentElement).toMatchSnapshot();

            example1Frame.remove();

            expect(document.querySelector(`.${cls.frame}`), 'Второй Iframe не удалился')
                .toBeNull();
            expect(document.documentElement).toMatchSnapshot();
        });

        it('Помечает загрузившиеся фреймы', () => {
            const framesContainerNode = createHtmlElement('div');
            document.body.appendChild(framesContainerNode);

            const exampleFrame = new Frame(framesContainerNode, 'https://example.com', root);
            const example1Frame = new Frame(framesContainerNode, 'https://example1.com', root);

            expect(exampleFrame.isLoaded()).toBe(false);
            expect(example1Frame.isLoaded()).toBe(false);

            const defaultEventSource = {} as MessageEventSource;

            Frame.handleShow({
                title: 'Турбо-страницы',
                cleanUrl: 'https://example.com',
                fixSwipe: false,
                url: 'https://example.com',
                originalUrl: 'https://test_news.com',
                source: exampleFrame.getElement().contentWindow || defaultEventSource,
                action: 'show',
            }, exampleFrame.getElement().contentWindow || defaultEventSource);

            expect(exampleFrame.isLoaded()).toBe(true);
            expect(example1Frame.isLoaded()).toBe(false);

            Frame.handleShow({
                title: 'Турбо-страницы',
                cleanUrl: 'https://example.com/1',
                fixSwipe: false,
                url: 'https://example.com/1',
                originalUrl: 'https://test_news.com',
                source: example1Frame.getElement().contentWindow || defaultEventSource,
                action: 'show',
            }, example1Frame.getElement().contentWindow || defaultEventSource);

            expect(exampleFrame.isLoaded()).toBe(true);
            expect(example1Frame.isLoaded()).toBe(true);
        });

        it('Устанавливает сдвиг фрейму', () => {
            const framesContainerNode = createHtmlElement('div');
            document.body.appendChild(framesContainerNode);

            const exampleFrame = new Frame(framesContainerNode, 'https://example.com', root);

            expect(framesContainerNode, 'Фрейм изначально не с нулевым смещением').toMatchSnapshot();

            exampleFrame.move('10px');

            expect(framesContainerNode, 'Фрейм не сместился на 10px').toMatchSnapshot();

            exampleFrame.move('100%');

            expect(framesContainerNode, 'Фрейм не сместился на 100%').toMatchSnapshot();
        });
    });
});
