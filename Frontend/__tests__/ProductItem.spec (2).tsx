import 'jest';
import * as React from 'react';
import { shallow, ShallowWrapper } from 'enzyme';

import {
    IProduct, ERecommendationPlace, ERecommendationCarouselType,
} from '~/types';
import { Button } from '~/components/Button';
import { ProductItem as ProductItemComponent } from '../ProductItemComponent';
import { ProductItemPresenter } from '../ProductItem';

jest.mock('~/libs/dataLayer');
jest.mock('@yandex-turbo/core/pubsub');
jest.mock('~/libs/Observer', () => {
    enum EObserveTypes {
        PRODUCT_LIST_AUTOLOAD,
    }

    let obs: Observer;
    class Observer {
        handleIntersection: (done: () => void) => void;
        constructor() {
            if (typeof obs === 'undefined') {
                obs = this;
            }
            return obs;
        }

        // @ts-ignore-next-line
        observe(_, { load: cb }) {
            if (!this.handleIntersection) {
                this.handleIntersection = cb;
            }
        }

        unobserve() {}

        done() {}

        intersect() {
            this.handleIntersection(this.done);
        }

        unobserveAll() {}
    }

    return { EObserveTypes, Observer };
});

const getDefaultMouseClickEvent = (): React.MouseEvent<HTMLButtonElement | HTMLAnchorElement> => ({
    ...new MouseEvent('click'),
    stopPropagation: jest.fn(),
    target: {} as HTMLButtonElement,
    currentTarget: {} as HTMLButtonElement,
    persist: jest.fn(),
    isDefaultPrevented: jest.fn(),
    isPropagationStopped: jest.fn(),
    nativeEvent: {} as MouseEvent,
    relatedTarget: {} as HTMLButtonElement,
});

const getDefaultProduct = (): IProduct => ({
    name: 'Apple iPad Pro 2020',
    price: {
        value: 80000,
    },
    id: 'productId',
    href: 'https://apple.com',
});

describe('ProductItem', () => {
    it('Рендерится без ошибок', () => {
        const addToCart = jest.fn(() => Promise.resolve());
        const showPopup = jest.fn(() => Promise.resolve());
        const product = getDefaultProduct();

        expect(() => shallow(
            <ProductItemPresenter
                count={1}
                shopId={'shopId'}
                id={'productId'}
                product={product}
                addToCart={addToCart}
                showPopup={showPopup}
            />
        )).not.toThrowError();
    });

    describe('Параметры baobab-событий', () => {
        const addToCart = jest.fn(() => Promise.resolve());
        const showPopup = jest.fn(() => Promise.resolve());
        const baobabOnIntersection = jest.fn();
        const baobabOnProductClick = jest.fn();
        const baobabOnAddToCart = jest.fn();
        let product: IProduct;

        beforeEach(() => {
            product = getDefaultProduct();
        });

        afterEach(() => {
            jest.clearAllMocks();
        });

        it('При первоначальном рендеринге не отправляем события просто так', () => {
            const wrapper = shallow(
                <ProductItemPresenter
                    count={1}
                    shopId={'shopId'}
                    id={'productId'}
                    product={product}
                    addToCart={addToCart}
                    showPopup={showPopup}
                    originalUrl={'https://original.ru/'}
                    mainOfferId={'main offer'}
                    place={ERecommendationPlace.MainPage}
                    recommendationType={ERecommendationCarouselType.Personal}

                    baobabOnIntersection={baobabOnIntersection}
                    baobabOnProductClick={baobabOnProductClick}
                    baobabOnAddToCart={baobabOnAddToCart}
                />
            );

            /** проверяем, что при первоначальном рендеринге не отправили ни одного события просто так */
            expect(baobabOnIntersection).not.toHaveBeenCalled();
            expect(baobabOnProductClick).not.toHaveBeenCalled();
            expect(baobabOnAddToCart).not.toHaveBeenCalled();

            wrapper.unmount();
        });

        it('НЕ отправляется баобаб-событие добавления в корзину для товара с внешней корзиной', () => {
            product.outerCartUrl = 'https://apple.com/checkout';
            const wrapper = shallow(
                <ProductItemPresenter
                    count={1}
                    shopId={'shopId'}
                    id={'productId'}
                    product={product}
                    addToCart={addToCart}
                    showPopup={showPopup}
                    originalUrl={'https://original.ru/'}
                    mainOfferId={'main offer'}
                    place={ERecommendationPlace.MainPage}
                    recommendationType={ERecommendationCarouselType.Personal}

                    baobabOnIntersection={baobabOnIntersection}
                    baobabOnProductClick={baobabOnProductClick}
                    baobabOnAddToCart={baobabOnAddToCart}
                />
            );

            const component = wrapper.find(ProductItemComponent);
            const actions = new ShallowWrapper(component.prop('actions'));
            const onClick = actions.find(Button).prop('onClick');
            const event = getDefaultMouseClickEvent();
            if (onClick) {
                onClick(event);
            }

            expect(baobabOnIntersection).not.toHaveBeenCalled();
            expect(baobabOnProductClick).not.toHaveBeenCalled();
            expect(baobabOnAddToCart).not.toHaveBeenCalled();

            wrapper.unmount();
        });
    });
});
