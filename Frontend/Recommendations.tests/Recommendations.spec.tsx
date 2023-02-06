import * as React from 'react';
import { shallow, mount } from 'enzyme';
import { createIntersectionObserverMock } from '@yandex-turbo/core/utils/intersectionObserverMock';
import { ViewportObserver, observers } from '@yandex-turbo/core/ViewportObserver';
import { FormatType } from '@yandex-turbo/components/Date/Date.types';
import { RecommendationsCard } from '@yandex-turbo/components/RecommendationsCard/RecommendationsCard';
import { isInExp } from '@yandex-turbo/core/experiments/experiments';
import pubsub from '@yandex-turbo/core/pubsub';
import { TAKE_CONTROL_EVENT } from '@yandex-turbo/core/sociality/sharedPubsubEvents';
import { IInterruption } from '@yandex-turbo/core/sociality/scrollManager';

import { Recommendations } from '../Recommendations';
import { OBSERVER_KEY } from '../Recommendations.types';
import { exampleProps } from './RecommendationsTestData';
import { recommendationsCn } from '../Recommendations.cn';
import { TurboCard } from '../../TurboCard/TurboCard';
import { RecommendationsCardAction } from '../../RecommendationsCard/Action/RecommendationsCard__Action';
import { IRecommendationsCardMetaProps } from '../../RecommendationsCard/Meta/RecommendationsCard__Meta.types';

jest.mock('@yandex-turbo/core/experiments/experiments');
jest.mock('@yandex-turbo/core/pubsub', () => {
    return {
        __esModule: true,
        default: {
            trigger: jest.fn(),
        },
    };
});
jest.useFakeTimers();

function flushPromises() {
    return new Promise(resolve => setImmediate(resolve));
}

function triggerViewportObserver(payload: IntersectionObserverEntry[]) {
    // @ts-ignore
    new ViewportObserver().callSubscribers(OBSERVER_KEY, payload, observers[OBSERVER_KEY]);
}

function createSocialPanelRoot() {
    const root = document.querySelector('.social-panel-root');
    if (!root) {
        const div = document.createElement('div');
        div.classList.add('social-panel-root');
        document.body.appendChild(div);
    }
}

function deleteSocialPanelRoot() {
    const root = document.querySelector('.social-panel-root');
    if (root && root.parentNode) {
        root.parentNode.removeChild(root);
    }
}

// Нужно поменять селектор, если будет использоваться другой тип карточки
// Например, у большой карточки div будет главным элементом
// Тип карточки сейчас задается в exampleProps, в поле type
const cardSelector = 'a.turbo-recommendations-card';

describe('Компонент Recommendations', () => {
    let counterMock: jest.Mock;
    let clearTimeoutSpy: jest.SpyInstance;

    beforeEach(() => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const g = global as any;

        g.w = jest.fn();
        g.IntersectionObserver = createIntersectionObserverMock();
        g.fetch = jest.fn(() => Promise.resolve(
            {
                ok: true,
                json: () => import('./RecommendationsTestData')
                    .then(module => (module.mockExampleResponse)),
            }
        ));
        g.Ya = {
            oncePageVisible: jest.fn(cb => cb()),
        };

        counterMock = g.w;
        clearTimeoutSpy = jest.spyOn(global, 'clearTimeout');

        jest.clearAllTimers();
        (isInExp as jest.Mock).mockReset();
        // ViewportObserver — синглтон и сохраняет состояние между тестами
        // Карточки и картинки отписываются на unmount но в тестах это нужно вызвать явно
        new ViewportObserver().unsubscribeAll(OBSERVER_KEY);
        (pubsub.trigger as jest.Mock).mockClear();
        window.history.replaceState({}, '');
    });

    test('Компонент ленты рендерится без ошибок', () => {
        const wrapper = shallow(<Recommendations {...exampleProps} />);

        expect(wrapper.exists()).toBe(true);

        expect(wrapper.hasClass(recommendationsCn()));
    });

    test('Компонент ленты рендерит карточки', async() => {
        const wrapper = mount(<Recommendations {...exampleProps} />);
        await flushPromises();
        wrapper.update();

        expect(wrapper.find(cardSelector).hostNodes()).toHaveLength(3);
    });

    test('Компонент ленты загружает и рендерит новую порцию карточек', async() => {
        const wrapper = mount(<Recommendations {...exampleProps} />);

        await flushPromises();
        wrapper.update();

        const cards = wrapper.find(cardSelector);

        // Делаем вид, что IntersectionObserver отработал для последней карточки
        triggerViewportObserver([
            {
                target: cards.last().getDOMNode(),
                isIntersecting: true,
                intersectionRatio: 0.0043,
            } as IntersectionObserverEntry,
        ]);

        await flushPromises();
        wrapper.update();

        expect(wrapper.find(cardSelector)).toHaveLength(6);
    });

    test('Доклеивает cgi параметры в ссылки карточек', async() => {
        const wrapper = mount(<Recommendations {...exampleProps} />);

        await flushPromises();
        wrapper.update();

        const cards = wrapper.find(cardSelector);
        const expectedQueryParams = [
            'parent_reqid=1592588619294035-999164734435750800300225-hamster-app-host-man-web-yp-66',
            'recommendation=true',
        ];

        cards.forEach(card => {
            const href = card.getElement().props.href;
            expectedQueryParams.forEach(param => {
                expect(href).toContain(param);
            });
        });
    });

    test('Отправляет счетчик видимости', async() => {
        const wrapper = mount(<Recommendations {...exampleProps} />);

        await flushPromises();
        wrapper.update();

        const cards = wrapper.find(cardSelector);
        const entries = cards
            .map((card, index) => {
                return {
                    target: card.getDOMNode(),
                    isIntersecting: index === 0,
                    intersectionRatio: index === 0 ? 1 : 0,
                };
            });

        triggerViewportObserver(entries as IntersectionObserverEntry[]);

        await flushPromises();
        wrapper.update();

        expect(counterMock).toBeCalledWith({
            event: 'tech',
            type: 'recommendations-card-visible',
            data: {
                position: 1,
                turboUrl: '/turbo?text=https%3A//medvisor.ru/articles/gripp-i-legochnye-zabolevaniya/pervye-dva-sluchaya-koronavirusa-v-rossii/',
                originalUrl: 'https://medvisor.ru/articles/gripp-i-legochnye-zabolevaniya/pervye-dva-sluchaya-koronavirusa-v-rossii/',
            },
        });
    });

    test('Отправляет счетчик heartbeat', async() => {
        const wrapper = mount<Recommendations>(<Recommendations {...exampleProps} />);

        // @ts-ignore — лезем в приватное поле, чтобы замокать метод, для простоты тестирования
        wrapper.instance().sendVisibilityEvent = jest.fn();

        await flushPromises();
        wrapper.update();

        const trigger = wrapper.find(cardSelector).last();

        // "Загрузим" немного карточек
        triggerViewportObserver([
            {
                target: trigger.last().getDOMNode(),
                isIntersecting: true,
                intersectionRatio: 0.0043,
            } as IntersectionObserverEntry,
        ]);

        await flushPromises();
        wrapper.update();

        const cards = wrapper.find(cardSelector);
        const entries = cards.map((card, idx) => {
            return {
                target: card.getDOMNode(),
                isIntersecting: idx === 0,
                intersectionRatio: idx === 0 ? 0.5 : 0,
            };
        });

        /**
         * Эмулируем такое состояние "видимости",
         * при котором считаем, что первая карточка перешла в состояние "seen",
         * а остальные еще не видны.
         */
        triggerViewportObserver(entries as IntersectionObserverEntry[]);

        await flushPromises();
        // В результате срабатывания callback должны запуститься таймеры heart beat
        jest.runOnlyPendingTimers();

        expect(counterMock).toBeCalledWith({
            event: 'tech',
            type: 'recommendations-heart-beat',
            data: {
                iteration: 0,
                position: 1,
                time: 1,
            },
        });

        jest.runOnlyPendingTimers();

        expect(counterMock).toBeCalledWith({
            event: 'tech',
            type: 'recommendations-heart-beat',
            data: {
                iteration: 1,
                position: 1,
                time: 1.6,
            },
        });

        jest.runOnlyPendingTimers();

        expect(counterMock).toBeCalledWith({
            event: 'tech',
            type: 'recommendations-heart-beat',
            data: {
                iteration: 2,
                position: 1,
                time: 2.56,
            },
        });

        const newEntries = [
            {
                target: cards.first().getDOMNode(),
                isIntersecting: true,
                intersectionRatio: 0.46,
            },
            {
                target: cards.at(1).getDOMNode(),
                isIntersecting: true,
                intersectionRatio: 0.5,
            },
        ] as IntersectionObserverEntry[];

        // @ts-ignore — лезем в приватное поле в целях тестирования
        const expectedTimeoutId = wrapper.instance().heartBeats.get(0);
        /**
         * Эмулируем такое состояние "видимости",
         * при котором триггер по threshold для первой карточки сработал
         * второй раз (это трактуется как выход из состояния вдимости),
         * а вторая карточка стала видна
         */
        triggerViewportObserver(newEntries);

        await flushPromises();
        wrapper.update();

        // Проверяем, что очистили таймаут
        expect(clearTimeoutSpy).toHaveBeenCalledTimes(1);
        expect(clearTimeoutSpy).toHaveBeenCalledWith(expectedTimeoutId);
        /**
         * В результате изменения состояния,
         * мы должны перестать следить за карточкой с индексом 0
         * и начать отправлять heartbeat для карточки с индексом 1
         */
        jest.runOnlyPendingTimers();

        expect(counterMock).toBeCalledWith({
            event: 'tech',
            type: 'recommendations-heart-beat',
            data: {
                iteration: 0,
                position: 2,
                time: 1,
            },
        });

        jest.runOnlyPendingTimers();

        expect(counterMock).toBeCalledWith({
            event: 'tech',
            type: 'recommendations-heart-beat',
            data: {
                iteration: 1,
                position: 2,
                time: 1.6,
            },
        });

        jest.runOnlyPendingTimers();

        expect(counterMock).toBeCalledWith({
            event: 'tech',
            type: 'recommendations-heart-beat',
            data: {
                iteration: 2,
                position: 2,
                time: 2.56,
            },
        });

        expect(counterMock).toHaveBeenCalledTimes(6);
    });

    test('Отправляет счетчик heartbeat сразу для нескольких карточек', async() => {
        const wrapper = mount<Recommendations>(<Recommendations {...exampleProps} />);

        // @ts-ignore — лезем в приватное поле, чтобы замокать метод, для простоты тестирования
        wrapper.instance().sendVisibilityEvent = jest.fn();

        await flushPromises();
        wrapper.update();

        const cards = wrapper.find(cardSelector);
        const trigger = cards.last();

        // "Загрузим" немного карточек
        triggerViewportObserver([
            {
                target: trigger.last().getDOMNode(),
                isIntersecting: true,
                intersectionRatio: 0.0043,
            } as IntersectionObserverEntry,
        ]);

        await flushPromises();
        wrapper.update();

        triggerViewportObserver([
            {
                target: cards.first().getDOMNode(),
                isIntersecting: true,
                intersectionRatio: 1,
            },
            {
                target: cards.at(1).getDOMNode(),
                isIntersecting: true,
                intersectionRatio: 0.5,
            },
        ] as IntersectionObserverEntry[]);

        await flushPromises();
        jest.runOnlyPendingTimers();

        expect(counterMock).toHaveBeenCalledTimes(2);
        expect(counterMock).toHaveBeenNthCalledWith(1, {
            event: 'tech',
            type: 'recommendations-heart-beat',
            data: {
                iteration: 0,
                position: 1,
                time: 1,
            },
        });
        expect(counterMock).toHaveBeenNthCalledWith(2, {
            event: 'tech',
            type: 'recommendations-heart-beat',
            data: {
                iteration: 0,
                position: 2,
                time: 1,
            },
        });
    });

    test('Отправляет счетчик click', async() => {
        const wrapper = mount(<Recommendations {...exampleProps} />);

        await flushPromises();
        wrapper.update();

        const card = wrapper.find(TurboCard).first();
        card.simulate('click');

        expect(counterMock).toBeCalledWith({
            event: 'tech',
            type: 'recommendation-card-click',
            data: {
                position: 1,
                turboUrl: '/turbo?text=https%3A//medvisor.ru/articles/gripp-i-legochnye-zabolevaniya/pervye-dva-sluchaya-koronavirusa-v-rossii/',
                originalUrl: 'https://medvisor.ru/articles/gripp-i-legochnye-zabolevaniya/pervye-dva-sluchaya-koronavirusa-v-rossii/',
            },
        });
    });

    test('Отправляет счетчик click один раз при клике в кнопку', async() => {
        const wrapper = mount(<Recommendations
            {...exampleProps}
        />);

        await flushPromises();
        wrapper.update();

        const cardAction = wrapper.find(RecommendationsCardAction).first();
        cardAction.simulate('click');

        expect(counterMock).toHaveBeenCalledTimes(1);
        expect(counterMock).toBeCalledWith({
            event: 'tech',
            type: 'recommendation-card-click',
            data: {
                position: 1,
                turboUrl: '/turbo?text=https%3A//medvisor.ru/articles/gripp-i-legochnye-zabolevaniya/pervye-dva-sluchaya-koronavirusa-v-rossii/',
                originalUrl: 'https://medvisor.ru/articles/gripp-i-legochnye-zabolevaniya/pervye-dva-sluchaya-koronavirusa-v-rossii/',
            },
        });
    });

    test('Очищает таймеры heartbeat при unmount', async() => {
        const mockedHeartBeat = new Map([
            [1, 2 as unknown as NodeJS.Timeout],
            [3, 4 as unknown as NodeJS.Timeout],
        ]);

        const wrapper = mount<Recommendations>(<Recommendations {...exampleProps} />);
        // @ts-ignore для простоты просто подменим хартбиты в приватном поле
        wrapper.instance().heartBeats = mockedHeartBeat;

        // @ts-ignore — лезем в приватное поле, чтобы следить за методом
        const stopHeartBeat = wrapper.instance().stopHeartBeat = jest.fn();

        await flushPromises();
        wrapper.update();
        wrapper.unmount();

        expect(stopHeartBeat).toBeCalledTimes(2);
        expect(stopHeartBeat).toHaveBeenNthCalledWith(1, 1);
        expect(stopHeartBeat).toHaveBeenNthCalledWith(2, 3);
    });

    test('Не прокидывает дату публикации по умолчанию', async() => {
        const wrapper = mount<Recommendations>(<Recommendations {...exampleProps} />);
        await flushPromises();
        wrapper.update();

        const cards = wrapper.find(RecommendationsCard);
        const metaPassed = cards.everyWhere(card => {
            const meta = card.prop<IRecommendationsCardMetaProps>('meta');

            return Boolean(meta && meta.date);
        });

        expect(metaPassed).toBe(false);
    });

    test('Прокидывает дату публикации, если передан withPubDate', async() => {
        const wrapper = mount<Recommendations>(<Recommendations {...exampleProps} withPubDate />);
        await flushPromises();
        wrapper.update();

        const cards = wrapper.find(RecommendationsCard);
        const metaPassed = cards.everyWhere(card => {
            const meta = card.prop<IRecommendationsCardMetaProps>('meta');

            return Boolean(meta && meta.date);
        });

        expect(metaPassed).toBe(true);
    });

    test('Форматирует дату публикации в зависимости от текущей даты', async() => {
        const wrapper = mount<Recommendations>(<Recommendations {...exampleProps} withPubDate />);

        await flushPromises();
        wrapper.update();

        const cards = wrapper.find(RecommendationsCard);
        const firstCardMeta = cards.first().prop<IRecommendationsCardMetaProps>('meta');
        const secondCardMeta = cards.at(1).prop<IRecommendationsCardMetaProps>('meta');

        expect(firstCardMeta.date, 'В первой карточке отсутсвует дата').toBeTruthy();
        expect(firstCardMeta.date!.type === FormatType.Default, 'Некорректно форматируется не сегодняшняя дата').toBe(true);

        expect(secondCardMeta.date, 'Во второй карточке отсутствует дата').toBeTruthy();
        expect(secondCardMeta.date!.type === FormatType.Time, 'Некорректно форматируется сегодняшняя дата');
    });

    test('Отписывается от viewport observer на unmount', async() => {
        const spy = jest.spyOn(new ViewportObserver(), 'unsubscribe');

        const wrapper = mount<Recommendations>(<Recommendations {...exampleProps} />);

        await flushPromises();
        wrapper.update();
        wrapper.unmount();

        expect(spy).toBeCalled();
    });

    test('Устанавливает shouldRenderAdvert в false по умолчанию', async() => {
        const wrapper = mount<Recommendations>(<Recommendations {...exampleProps} />);

        expect(wrapper.state('shouldRenderAdvert')).toEqual(false);
    });

    test(`Отправляет событие ${TAKE_CONTROL_EVENT}, когда видно ленту рекомендаций`, async() => {
        createSocialPanelRoot();
        const interruptionMock: IInterruption = {
            hide: jest.fn(),
            show: jest.fn(),
            continue: jest.fn(),
        };

        (pubsub.trigger as jest.Mock).mockImplementationOnce((_, cb) => cb(Promise.resolve(interruptionMock)));
        const wrapper = mount(<Recommendations {...exampleProps} />);

        jest.runAllTimers(); // Даём пройти всем raf
        await flushPromises();
        wrapper.update();

        const cards = wrapper.find(cardSelector);

        triggerViewportObserver([
            {
                target: wrapper.getDOMNode(),
                isIntersecting: true,
            } as IntersectionObserverEntry,
            {
                target: cards.first().getDOMNode(),
                isIntersecting: true,
                intersectionRatio: 0.0043,
            } as IntersectionObserverEntry,
        ]);

        jest.runAllTimers(); // Даём пройти всем raf
        wrapper.update();

        expect(pubsub.trigger).toBeCalledTimes(1);
        expect(pubsub.trigger).toBeCalledWith(TAKE_CONTROL_EVENT, expect.any(Function));
        await flushPromises();
        expect(interruptionMock.hide).toBeCalled();
    });

    test('Возвращает контроль в плашку, когда покидает viewport', async() => {
        createSocialPanelRoot();
        const interruptionMock: IInterruption = {
            hide: jest.fn(),
            show: jest.fn(),
            continue: jest.fn(),
        };
        (pubsub.trigger as jest.Mock).mockImplementationOnce((_, cb) => cb(Promise.resolve(interruptionMock)));

        const wrapper = mount(<Recommendations {...exampleProps} />);

        await flushPromises();
        wrapper.update();

        // Симулируем вызов коллбека Observer при его инициализации.
        // В этом случае событие не должно отправляться.
        triggerViewportObserver([
            {
                target: wrapper.getDOMNode(),
                isIntersecting: false,
            } as IntersectionObserverEntry,
        ]);

        jest.runAllTimers(); // Даём пройти всем raf
        wrapper.update();

        expect(pubsub.trigger).not.toBeCalled();
        expect(interruptionMock.continue).not.toBeCalled();

        // Лента стала видна, теперь, если мы уйдём из viewport
        // событие SHOW_EVENT должно отправится
        triggerViewportObserver([
            {
                target: wrapper.getDOMNode(),
                isIntersecting: true,
            } as IntersectionObserverEntry,
        ]);

        jest.runAllTimers(); // Даём пройти всем raf
        wrapper.update();

        // Лента стала видна, теперь, если мы уйдём из viewport
        // событие SHOW_EVENT должно отправится
        triggerViewportObserver([
            {
                target: wrapper.getDOMNode(),
                isIntersecting: false,
            } as IntersectionObserverEntry,
        ]);

        jest.runAllTimers(); // Даём пройти всем raf
        wrapper.update();

        await flushPromises();
        expect(interruptionMock.continue).toBeCalled();
    });

    test('Не пытается забрать контроль над плашкой, если её корневого элемента нет на странице', async() => {
        deleteSocialPanelRoot();
        const interruptionMock: IInterruption = {
            hide: jest.fn(),
            show: jest.fn(),
            continue: jest.fn(),
        };

        (pubsub.trigger as jest.Mock).mockImplementationOnce((_, cb) => cb(Promise.resolve(interruptionMock)));
        const wrapper = mount(<Recommendations {...exampleProps} />);

        jest.runAllTimers(); // Даём пройти всем raf
        await flushPromises();
        wrapper.update();

        const cards = wrapper.find(cardSelector);

        triggerViewportObserver([
            {
                target: wrapper.getDOMNode(),
                isIntersecting: true,
            } as IntersectionObserverEntry,
            {
                target: cards.first().getDOMNode(),
                isIntersecting: true,
                intersectionRatio: 0.0043,
            } as IntersectionObserverEntry,
        ]);

        jest.runAllTimers(); // Даём пройти всем raf
        wrapper.update();

        expect(pubsub.trigger).not.toBeCalled();
    });

    test('Ограничивает число карточек, если передан параметр cardsLimit', async() => {
        const wrapper = mount(<Recommendations {...exampleProps } {...{ cardsLimit: 2 }} />);
        await flushPromises();
        wrapper.update();

        expect(wrapper.find(cardSelector).hostNodes()).toHaveLength(2);
    });
});
