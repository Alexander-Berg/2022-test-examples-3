import * as React from 'react';
import { shallow, mount } from 'enzyme';
import { act } from 'react-dom/test-utils';
import { EStorageTypes } from '@yandex-turbo/core/utils/recommendations/appropriateImageSource';
import { getAppropriateImageSrc } from '@yandex-turbo/core/utils/recommendations/appropriateImageSource';
import { ViewportObserver, observers } from '@yandex-turbo/core/ViewportObserver';
import { createIntersectionObserverMock } from '@yandex-turbo/core/utils/intersectionObserverMock';
import { useVisibilityProvider } from '../RecommendationsCard__Image.hooks';

import { OBSERVER_KEY } from '../RecommendationsCard__Image.hooks';
import { IRecommendationsCardImageProps } from '../RecommendationsCard__Image.types';
import { RecommendationsCardImage } from '../RecommendationsCard__Image';
import { recommendationsCardCn } from '../../RecommendationsCard.cn';

jest.mock('@yandex-turbo/core/utils/recommendations/appropriateImageSource');

function triggerViewportObserver(payload: IntersectionObserverEntry[]) {
    act(() => {
        // @ts-ignore
        new ViewportObserver().callSubscribers(OBSERVER_KEY, payload, observers[OBSERVER_KEY]);
    });
}

function mountAndIntersect(props: Partial<IRecommendationsCardImageProps> = {}, imageUrl?: string) {
    (getAppropriateImageSrc as jest.Mock).mockImplementationOnce(() => imageUrl || 'https://yandex.ru/image');

    const wrapper = mount(
        <RecommendationsCardImage
            height={625}
            width={506}
            src="http://im2-tub-com.yandex.net/i?id=828719948d4c15bbfe32b27f9c377fdd&ref=itditp&n=4&w=625&h=506"
            storage={EStorageTypes.im2tub}
            caption="Фото: Виктор Коротаев / Forbes"
            projectId="get-turbo"
            imageSizes={{ '244x122': '/max_g480_c6_r16x9_pd10' }}
            {...props}
        />
    );

    expect(wrapper.find('img').length).toEqual(0);

    triggerViewportObserver([
        {
            target: wrapper.find('figure').getDOMNode(),
            isIntersecting: true,
        } as IntersectionObserverEntry,
    ]);
    wrapper.update();

    return wrapper;
}

describe('Компонент RecommendationsCardImage', () => {
    beforeEach(() => {
        jest.resetAllMocks();

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const g = global as any;

        g.Ya = {
            experiments: {},
            isInFrame: jest.fn(),
        };
        g.IntersectionObserver = createIntersectionObserverMock();

        new ViewportObserver().unsubscribeAll(OBSERVER_KEY);
    });

    afterAll(() => {
        // @ts-ignore
        delete global.Ya;
    });

    test('Картинка с "сырым" URL рендерится без ошибок', () => {
        const wrapper = shallow(
            <RecommendationsCardImage
                height={1200}
                width={600}
                src="https://example.com"
                storage={EStorageTypes.other}
                projectId="get-turbo"
                imageSizes={{ '244x122': '/max_g480_c6_r16x9_pd10' }}
            />
        );

        expect(wrapper.exists()).toBe(true);
    });

    test('Картинка с URL avatars рендерится без ошибок', () => {
        const wrapper = shallow(
            <RecommendationsCardImage
                height={1200}
                width={600}
                src="https://avatars.mds.yandex.net/get-turbo/1357097/ee119c00a860bf3e6bb515ef5d049732/orig"
                storage={EStorageTypes.avatars}
                projectId="get-turbo"
                imageSizes={{ '244x122': '/max_g480_c6_r16x9_pd10' }}
            />
        );

        expect(wrapper.exists()).toBe(true);
    });

    test('Картинка с URL тумблейлера рендерится без ошибок', () => {
        const wrapper = shallow(
            <RecommendationsCardImage
                height={625}
                width={506}
                src="http://im2-tub-com.yandex.net/i?id=828719948d4c15bbfe32b27f9c377fdd&ref=itditp&n=4&w=625&h=506"
                storage={EStorageTypes.im2tub}
                projectId="get-turbo"
                imageSizes={{ '244x122': '/max_g480_c6_r16x9_pd10' }}
            />
        );

        expect(wrapper.exists()).toBe(true);
    });

    test('Картинка рендерится с размером m по умолчанию', () => {
        const wrapper = shallow(
            <RecommendationsCardImage
                height={625}
                width={506}
                src="http://im2-tub-com.yandex.net/i?id=828719948d4c15bbfe32b27f9c377fdd&ref=itditp&n=4&w=625&h=506"
                storage={EStorageTypes.im2tub}
                projectId="get-turbo"
                imageSizes={{ '244x122': '/max_g480_c6_r16x9_pd10' }}
            />
        );

        const className = recommendationsCardCn('image-content', { size: 'm' });
        const content = wrapper.find(`.${recommendationsCardCn('image-content')}`);
        expect(content.hasClass(className)).toBe(true);
    });

    test('Картинка рендерится с размером l', () => {
        const wrapper = shallow(
            <RecommendationsCardImage
                height={625}
                width={506}
                size="l"
                src="http://im2-tub-com.yandex.net/i?id=828719948d4c15bbfe32b27f9c377fdd&ref=itditp&n=4&w=625&h=506"
                storage={EStorageTypes.im2tub}
                projectId="get-turbo"
                imageSizes={{ '244x122': '/max_g480_c6_r16x9_pd10' }}
            />
        );

        const className = recommendationsCardCn('image-content', { size: 'l' });
        const content = wrapper.find(`.${recommendationsCardCn('image-content')}`);
        expect(content.hasClass(className)).toBe(true);
    });

    describe('Компонент картинки вызывает утилиту преобразования URL', () => {
        test('get-turbo ns', () => {
            const wrapper = mount(
                <RecommendationsCardImage
                    height={625}
                    width={506}
                    src="http://im2-tub-com.yandex.net/i?id=828719948d4c15bbfe32b27f9c377fdd&ref=itditp&n=4&w=625&h=506"
                    storage={EStorageTypes.im2tub}
                    projectId="get-turbo"
                    imageSizes={{ '244x122': '/max_g480_c6_r16x9_pd10' }}
                />
            );

            const node = wrapper.getDOMNode();

            expect(getAppropriateImageSrc).toBeCalledWith({
                viewportWidth: window.innerWidth,
                isRetina: false,
                imageHeight: 625,
                imageWidth: 506,
                storage: EStorageTypes.im2tub,
                src: 'http://im2-tub-com.yandex.net/i?id=828719948d4c15bbfe32b27f9c377fdd&ref=itditp&n=4&w=625&h=506',
                // @ts-ignore — непонятно, почему нет offsetWith и offsetHeigh
                boxHeight: node.offsetWidth,
                // @ts-ignore
                boxWidth: node.offsetHeight,
                projectId: 'get-turbo',
                imageSizes: { '244x122': '/max_g480_c6_r16x9_pd10' },
            });
        });

        test('get-snippets_images ns', () => {
            const wrapper = mount(
                <RecommendationsCardImage
                    height={625}
                    width={506}
                    src="http://im2-tub-com.yandex.net/i?id=828719948d4c15bbfe32b27f9c377fdd&ref=itditp&n=4&w=625&h=506"
                    storage={EStorageTypes.avatars}
                    projectId="get-snippets_images"
                    imageSizes={{ '244x122': '/max_g480_c6_r16x9_pd10' }}
                />
            );

            const node = wrapper.getDOMNode();

            expect(getAppropriateImageSrc).toBeCalledWith({
                viewportWidth: window.innerWidth,
                isRetina: false,
                imageHeight: 625,
                imageWidth: 506,
                storage: EStorageTypes.avatars,
                src: 'http://im2-tub-com.yandex.net/i?id=828719948d4c15bbfe32b27f9c377fdd&ref=itditp&n=4&w=625&h=506',
                // @ts-ignore — непонятно, почему нет offsetWith и offsetHeigh
                boxHeight: node.offsetWidth,
                // @ts-ignore
                boxWidth: node.offsetHeight,
                projectId: 'get-snippets_images',
                imageSizes: { '244x122': '/max_g480_c6_r16x9_pd10' },
            });
        });

        test('others ns', () => {
            const wrapper = mount(
                <RecommendationsCardImage
                    height={625}
                    width={506}
                    src="http://im2-tub-com.yandex.net/i?id=828719948d4c15bbfe32b27f9c377fdd&ref=itditp&n=4&w=625&h=506"
                    storage={EStorageTypes.avatars}
                    projectId="get-ynews"
                    imageSizes={{ '244x122': '/max_g480_c6_r16x9_pd10' }}
                />
            );

            const node = wrapper.getDOMNode();

            expect(getAppropriateImageSrc).toBeCalledWith({
                viewportWidth: window.innerWidth,
                isRetina: false,
                imageHeight: 625,
                imageWidth: 506,
                storage: EStorageTypes.avatars,
                src: 'http://im2-tub-com.yandex.net/i?id=828719948d4c15bbfe32b27f9c377fdd&ref=itditp&n=4&w=625&h=506',
                // @ts-ignore — непонятно, почему нет offsetWith и offsetHeigh
                boxHeight: node.offsetWidth,
                // @ts-ignore
                boxWidth: node.offsetHeight,
                projectId: 'get-ynews',
                imageSizes: { '244x122': '/max_g480_c6_r16x9_pd10' },
            });
        });
    });

    test('Компонент картинки рендерит аннотацию к изображению', () => {
        const wrapper = mount(
            <RecommendationsCardImage
                height={625}
                width={506}
                src="http://im2-tub-com.yandex.net/i?id=828719948d4c15bbfe32b27f9c377fdd&ref=itditp&n=4&w=625&h=506"
                storage={EStorageTypes.im2tub}
                caption="Фото: Виктор Коротаев / Forbes"
                projectId="get-turbo"
                imageSizes={{ '244x122': '/max_g480_c6_r16x9_pd10' }}
            />
        );
        const caption = wrapper.find(`.${recommendationsCardCn('image-caption')}`);

        expect(caption.exists()).toBe(true);
    });

    test('Компонент картинки возвращает null, если произошла ошибка загрузки', () => {
        const wrapper = mountAndIntersect();

        const img = wrapper.find('img');
        img.simulate('error');
        wrapper.update();

        expect(wrapper.isEmptyRender()).toBe(true);
    });

    test('Компонент картинки загружается, когда срабатывает callback ViewportObserver', () => {
        const wrapper = mountAndIntersect();

        const img = wrapper.find('img');
        expect(img.exists()).toBe(true);
    });

    test('Компонент картинки отображает изображение, только когда оно загрузилась', () => {
        const wrapper = mountAndIntersect();
        const findImgWrapper = () => wrapper
            .find(`.${recommendationsCardCn('image-simple')}`)
            .getDOMNode() as HTMLElement;

        expect(findImgWrapper().style.backgroundImage).toBeFalsy();
        const img = wrapper.find('img');
        img.simulate('load');
        wrapper.update();

        expect(findImgWrapper().style.backgroundImage).toBe('url(https://yandex.ru/image)');
    });

    test('useVisibilityProvider устанавливает page__container как root', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const isInFrame = (global as any).Ya.isInFrame as jest.Mock;
        isInFrame.mockReturnValueOnce(true);

        const querySpy = jest.spyOn(document, 'querySelector');
        querySpy.mockImplementationOnce(() => {
            const container = document.createElement('div');
            container.classList.add('page__container');

            return container;
        });

        const viewportObserverSpy = jest.spyOn(new ViewportObserver(), 'create');

        function Test() {
            const elem = document.createElement('div');
            useVisibilityProvider(elem);

            return null;
        }

        mount(<Test />);

        expect(viewportObserverSpy).toBeCalledWith(expect.anything(), { root: querySpy.mock.results[0].value, rootMargin: '0px 0px 100% 0px' });
    });
});
