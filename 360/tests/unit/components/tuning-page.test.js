import React from 'react';
import { shallow } from 'enzyme';
import '../noscript';

import { Button } from '@ps-int/ufo-rocks/lib/components/lego-components/Button';

jest.mock('../../../components/redux/components/tuning-page/helpers', () => ({
    metrikaCount: jest.fn()
}));
import { metrikaCount } from '../../../components/redux/components/tuning-page/helpers';
import { TuningPage } from '../../../components/redux/components/tuning-page/tuning-page';
import { TuningPageHistory } from '../../../components/redux/components/tuning-page/history';
import BuyButton from '../../../components/redux/components/tuning-page/buy-button';
import BuyLink from '../../../components/redux/components/tuning-page/buy-link';
import { Features } from '../../../components/redux/components/tuning-page/features';
import { Title } from '../../../components/redux/components/tuning-page/title';
import { Tariff } from '../../../components/redux/components/tuning-page/tariff';
import { TariffContainer } from '../../../components/redux/components/tuning-page/tariff-container';
import {
    isThreeTariffsExperiment,
    getSpaceTariffItems,
    isYaPlusTariffPresent,
    hasDiscount,
    isProEnabled,
    getSpace,
    isLoading,
    isError,
    getPosition
} from '../../../components/redux/components/tuning-page/selectors';

describe('TuningPage', () => {
    const getWrapperFunctor = (Component) => (props = {}) => shallow(<Component {...props} />);
    const getPropsFunctor = (defaultProps = {}) => (props = {}) => Object.assign({}, defaultProps, props);
    const spaceTariffItems = [
        {
            currency: 'RUB',
            discount: {
                percentage: 30
            },
            display_space: 100,
            display_space_units: 'ГБ',
            space: 107374182400,
            periods: {
                month: {
                    price: 99,
                    product_id: '100gb_1m_2019_v4_discount_30'
                },
                year: {
                    price: 984,
                    product_id: '100gb_1y_2019_v4_discount_30'
                }
            }
        },
        {
            discount: {
                percentage: 30
            },
            space: 1099511627776,
            display_space_units: 'ТБ',
            currency: 'RUB',
            is_best_offer: true,
            periods: {
                year: {
                    price: 1750,
                    product_id: '1tb_1y_2019_v4_discount_30'
                },
                month: {
                    price: 210,
                    product_id: '1tb_1m_2019_v4_discount_30'
                }
            },
            display_space: 1
        },
        {
            discount: {
                percentage: 30
            },
            space: 1099511627776,
            display_space_units: 'ТБ',
            currency: 'RUB',
            is_yandex_plus: true,
            periods: {
                year: {
                    price: 5244,
                    product_id: '1tb_1y_2019_v4_discount_30'
                },
                month: {
                    price: 750,
                    product_id: '1tb_1m_2019_v4_discount_30'
                }
            },
            display_space: 3
        }
    ];
    describe('TuningPage Component', () => {
        const getWrapper = getWrapperFunctor(TuningPage);

        const defaultProps = {
            packagesSpace: [],
            spaceTariffItems,
            orders: [],
            isLoading: false,
            isError: false,
            isProEnabled: false,
            spaceLimit: 100,
            spaceUsed: 0,
            spaceFree: 100,
            percentageOfUsed: 0,
            fetchPackagesSpace: jest.fn(),
            fetchSpaceTariffs: jest.fn(),
            fetchOrders: jest.fn()
        };
        const getProps = getPropsFunctor(defaultProps);

        test('should display LoaderOrError Component if is loading', () => {
            const props = getProps({ isLoading: true, spaceTariffItems: [] });
            const wrapper = getWrapper(props);

            expect(wrapper).toMatchSnapshot();
            expect(props.fetchPackagesSpace).toHaveBeenCalled();
            expect(props.fetchSpaceTariffs).toHaveBeenCalled();
            expect(props.fetchOrders).toHaveBeenCalled();
        });

        test('should display LoaderOrError Component if is error', () => {
            const props = getProps({ isError: true });

            const wrapper = getWrapper(props);

            expect(wrapper).toMatchSnapshot();
        });

        test('should render tuning page for a non pro user', () => {
            const props = getProps();

            const wrapper = getWrapper(props);

            expect(wrapper).toMatchSnapshot();
            expect(metrikaCount).toHaveBeenCalledWith('show', 'pro disable');
        });

        test('should render tuning page for a pro user', () => {
            const props = getProps({ isProEnabled: true });

            const wrapper = getWrapper(props);

            expect(wrapper).toMatchSnapshot();
            expect(metrikaCount).toHaveBeenCalledWith('show', 'pro enable');
        });

        test('_onDiscountShow should be called', () => {
            let props = getProps({ spaceTariffItems: [] });
            const wrapper = getWrapper(props);

            const onDiscountShowSpy = jest.spyOn(wrapper.instance(), '_onDiscountShow');

            props = getProps();
            wrapper.setProps(props);

            expect(onDiscountShowSpy).toHaveBeenCalled();
            expect(metrikaCount).toHaveBeenCalledWith('discount', spaceTariffItems[0].discount.percentage);
        });
    });

    [
        { component: BuyButton, name: 'BuyButton' },
        { component: BuyLink, name: 'BuyLink' }
    ].forEach(({ component, name }) => {
        describe(`${name} Component`, () => {
            const getWrapper = getWrapperFunctor(component);
            const defaultProps = {
                tariff: spaceTariffItems[0],
                diskThreeTariffsExperiment: true,
                onClick: jest.fn()
            };
            const getProps = getPropsFunctor(defaultProps);

            test('should render default', () => {
                const props = getProps();
                const wrapper = getWrapper(props);

                expect(wrapper).toMatchSnapshot();
            });

            test('should render best offer', () => {
                const props = getProps({ tariff: spaceTariffItems[1] });
                const wrapper = getWrapper(props);

                expect(wrapper).toMatchSnapshot();
            });

            test('should render with ya plus', () => {
                const props = getProps({ tariff: spaceTariffItems[2] });
                const wrapper = getWrapper(props);

                expect(wrapper).toMatchSnapshot();
            });
        });
    });

    describe('Features Component', () => {
        const getWrapper = getWrapperFunctor(Features);
        const defaultProps = {
            isProEnabled: false,
            isYaPlusTariffPresent: false,
            servicesUrls: 'https://plus.yandex.ru',
            isTariffFeatures: true
        };
        const getProps = getPropsFunctor(defaultProps);

        test('should render null', () => {
            [
                { isTariffFeatures: false },
                { isProEnabled: true, isYaPlusTariffPresent: false }
            ]
                .map(getProps)
                .map(getWrapper)
                .forEach((wrapper) => {
                    expect(wrapper.isEmptyRender()).toBe(true);
                });
        });

        test('should render features for non pro without ya plus tariff present', () => {
            const props = getProps();
            const wrapper = getWrapper(props);

            expect(wrapper).toMatchSnapshot();
        });

        test('should render features for non pro with ya plus tariff present', () => {
            const props = getProps({ isYaPlusTariffPresent: true });
            const wrapper = getWrapper(props);

            expect(wrapper).toMatchSnapshot();
        });
    });

    describe('Title Component', () => {
        const getWrapper = getWrapperFunctor(Title);
        const defaultProps = {
            spaceTariffItems: spaceTariffItems.map((tariff) => Object.assign({}, tariff, { discount: undefined })),
            hasYaPlus: false,
            isProEnabled: false
        };
        const getProps = getPropsFunctor(defaultProps);

        test('should render Title for non pro user without ya plus and no discount', () => {
            const wrapper = getWrapper(getProps());

            expect(wrapper).toMatchSnapshot();
        });

        test('should render Title for a pro user without ya plus and no discount', () => {
            const wrapper = getWrapper(getProps({ isProEnabled: true }));

            expect(wrapper).toMatchSnapshot();
        });

        test('should render Title for a non pro user without ya plus with discount', () => {
            const wrapper = getWrapper(getProps({ spaceTariffItems }));

            expect(wrapper).toMatchSnapshot();
        });

        test('should render Title for a non pro user with ya plus with discount', () => {
            const wrapper = getWrapper(getProps({ hasYaPlus: true, spaceTariffItems }));

            expect(wrapper).toMatchSnapshot();
        });

        test('should render Title for a non pro user with ya plus with a limited discount', () => {
            const wrapper = getWrapper(getProps({
                spaceTariffItems: spaceTariffItems.map((tariff) => Object.assign({}, tariff, { discount: { active_until_ts: 1234567891011 } }))
            }));

            expect(wrapper).toMatchSnapshot();
        });
    });

    describe('Tariff Container Component', () => {
        const getWrapper = getWrapperFunctor(TariffContainer);
        const defaultProps = {
            spaceTariffItems: spaceTariffItems.map((tariff) => Object.assign({}, tariff, { discount: undefined })),
            hasYaPlus: false,
            isProEnabled: false
        };
        const getProps = getPropsFunctor(defaultProps);

        // iPhone X
        global.document = { documentElement: { } };
        Object.defineProperty(global.document.documentElement, 'clientWidth', { value: 375 });
        const getBoundingClientRectByX = (x) => {
            return {
                getBoundingClientRect: () => {
                    return {
                        x,
                        width: 339
                    };
                }
            };
        };

        const wrapper = getWrapper(getProps());
        const wrapperInstance = wrapper.instance();
        wrapperInstance._containerRef = {
            scrollWidth: 1061,
            children: [getBoundingClientRectByX(0), getBoundingClientRectByX(361), getBoundingClientRectByX(704)]
        };

        test('scroll position should be 0 for position = 0, 1, < -spaceTariffItems.length', () => {
            expect(wrapperInstance._getTariffScrollByPosition(0)).toBe(0);
            expect(wrapperInstance._getTariffScrollByPosition(1)).toBe(0);
            expect(wrapperInstance._getTariffScrollByPosition(-10)).toBe(0);
        });

        test('scroll position should be > 0 by default (nonInteger) and from the second to the last tariff', () => {
            expect(wrapperInstance._getTariffScrollByPosition(undefined)).toBe(343);
            expect(wrapperInstance._getTariffScrollByPosition(5.45)).toBe(343);
            expect(wrapperInstance._getTariffScrollByPosition(2)).toBe(343);
            expect(wrapperInstance._getTariffScrollByPosition(-1)).toBe(686);
        });
    });

    describe('Tariff Component', () => {
        const getWrapper = getWrapperFunctor(Tariff);
        const defaultProps = {
            isProEnabled: false,
            spaceLimit: 100000000000,
            diskThreeTariffsExperiment: true,
            paymentPay: jest.fn(),
            openDialog: jest.fn()
        };
        const getProps = getPropsFunctor(defaultProps);

        [true, false].forEach((isProEnabled) => {
            spaceTariffItems.forEach((tariff, index) => {
                test(`should render Tariff component for tariff #${index + 1} for a user with pro ${isProEnabled ? 'enabled' : 'disabled'}`, () => {
                    const props = getProps({ tariff, isProEnabled });
                    const wrapper = getWrapper(props);

                    expect(wrapper).toMatchSnapshot();

                    const buyButton = wrapper.find(BuyButton);

                    buyButton.simulate('click');

                    expect(props.paymentPay).toHaveBeenCalledTimes(1);
                    expect(props.openDialog).toHaveBeenCalledTimes(1);

                    const buyLink = wrapper.find(BuyLink);
                    buyLink.simulate('click');

                    expect(props.paymentPay).toHaveBeenCalledTimes(2);
                    expect(props.openDialog).toHaveBeenCalledTimes(2);

                    jest.resetAllMocks();
                });
            });
        });
    });

    describe('History Component', () => {
        const getWrapper = getWrapperFunctor(TuningPageHistory);
        const defaultProps = {
            supportLink: 'https://yandex.ru/support',
            packagesSpace: [
                {
                    payment_method: 'GOOGLE_PLAY',
                    expires: 1598714258,
                    removes: 1598714258,
                    free: false,
                    ctime: 1567091868,
                    size: 1099511627776,
                    name: '1tb_1y_2019_v4',
                    state: null,
                    sid: 'd3f505bef8769337e1665e6687285e39',
                    subscription: true,
                    order: '4ad25877381248d3bdc0380833e07698',
                    title: '+ 1 Тб на год в Google Play',
                    id: 43
                },
                {
                    payment_method: 'APPLE_APPSTORE',
                    expires: 1598714541,
                    removes: 1598714541,
                    free: false,
                    ctime: 1567092144,
                    size: 107374182400,
                    name: '100gb_1y_2019_v4',
                    state: null,
                    sid: '6e4d8685c8187dab4910f9b323c05eb5',
                    subscription: true,
                    order: 'faf14e37b30d4a088cded2f77e32fcb3',
                    title: '+ 100 Гб на год в App Store',
                    id: 44
                },
                {
                    expires: null,
                    removes: null,
                    free: true,
                    ctime: 0,
                    size: 10737418240,
                    name: 'initial_10gb',
                    state: null,
                    sid: '6818c6c8ace3362b4b13cd775c0f56cb',
                    subscription: false,
                    order: null,
                    title: 'За регистрацию Диска',
                    id: 42
                }
            ],
            orders: [],
            openDialog: jest.fn()
        };
        const getProps = getPropsFunctor(defaultProps);

        test('should render two button-links to support for in app purchases', () => {
            const props = getProps();
            const wrapper = getWrapper(props);

            expect(wrapper).toMatchSnapshot();

            expect(wrapper.find(Button).length).toBe(2);

            wrapper.find(Button).first().simulate('click');
            expect(metrikaCount).toHaveBeenCalledWith('more google');

            wrapper.find(Button).last().simulate('click');
            expect(metrikaCount).toHaveBeenCalledWith('more ios');
        });
    });

    describe('Selectors', () => {
        const getState = (state = {}) => Object.assign({}, {
            space: {
                limit: 100,
                used: 0,
                free: 100
            },
            user: {
                features: {
                    disk_pro: {
                        enabled: false
                    }
                }
            },
            spaceTariffs: {
                items: []
            }
        }, state);

        describe('isThreeTariffsExperiment', () => {
            test('should not fail on empty data', () => {
                expect(isThreeTariffsExperiment({})).toBe(undefined);
            });

            test('should return false', () => {
                expect(isThreeTariffsExperiment(getState())).toBe(false);
                expect(isThreeTariffsExperiment(getState({ spaceTariffs: { items: [1, 2] } }))).toBe(false);
            });

            test('should return true', () => {
                expect(isThreeTariffsExperiment(getState({ spaceTariffs: { items: [1, 2, 3] } }))).toBe(true);
            });
        });

        describe('getSpaceTariffItems', () => {
            test('should not fail on empty data', () => {
                expect(getSpaceTariffItems({}).length).toBe(0);
            });

            test('should return space tariff items', () => {
                [
                    [1],
                    [1, 2],
                    [1, 2, 3],
                    [1, 2, 3, 4]
                ].forEach((items) => {
                    expect(getSpaceTariffItems(getState({ spaceTariffs: { items } })).length).toBe(Math.min(items.length, 3));
                });
            });
        });

        describe('isYaPlusTariffPresent', () => {
            test('should not fail on empty data', () => {
                expect(isYaPlusTariffPresent({})).toBe(undefined);
            });

            test('should return false', () => {
                [
                    [{}],
                    [{}, {}],
                    [{}, {}, {}]
                ].forEach((items) => {
                    expect(isYaPlusTariffPresent(getState({ spaceTariffs: { items } }))).toBe(false);
                });
            });

            test('should return true', () => {
                [
                    [{ is_yandex_plus: true }],
                    [{}, { is_yandex_plus: true }],
                    [{}, {}, { is_yandex_plus: true }]
                ].forEach((items) => {
                    expect(isYaPlusTariffPresent(getState({ spaceTariffs: { items } }))).toBe(true);
                });
            });
        });

        describe('hasDiscount', () => {
            test('should not fail on empty data', () => {
                expect(hasDiscount({})).toBe(undefined);
            });

            test('should return false', () => {
                [
                    [{}],
                    [{}, {}],
                    [{}, {}, {}]
                ].forEach((items) => {
                    expect(hasDiscount(getState({ spaceTariffs: { items } }))).toBe(false);
                });
            });

            test('should return true', () => {
                [
                    [{ discount: {} }],
                    [{}, { discount: {} }],
                    [{}, {}, { discount: {} }]
                ].forEach((items) => {
                    expect(hasDiscount(getState({ spaceTariffs: { items } }))).toBe(true);
                });
            });
        });

        describe('isProEnabled', () => {
            test('should not fail on empty data', () => {
                expect(isProEnabled({})).toBe(undefined);
            });

            test('should return false', () => {
                expect(isProEnabled(getState())).toBe(false);
            });

            test('should return true', () => {
                expect(isProEnabled(getState({ user: { features: { disk_pro: { enabled: true } } } }))).toBe(true);
            });
        });

        describe('getSpace', () => {
            test('should get space from the store', () => {
                expect(getSpace(getState())).toEqual({ spaceLimit: 100, spaceUsed: 0, spaceFree: 100 });
            });
        });

        describe('isLoading', () => {
            test('should return true', () => {
                expect(isLoading(getState({ packagesSpace: { state: 'loading' } }))).toBe(true);
                expect(isLoading(getState({ spaceTariffs: { items: undefined }, packagesSpace: { state: 'loaded' } }))).toBe(true);
            });

            test('should return false', () => {
                expect(isLoading(getState({ spaceTariffs: { items: [] }, packagesSpace: { state: 'loaded' } }))).toBe(false);
            });
        });

        describe('isError', () => {
            test('should return true', () => {
                expect(isError(getState({ packagesSpace: { state: 'error' } }))).toBe(true);
                expect(isError(getState({ spaceTariffs: { error: {} } }))).toBe(true);
            });

            test('should return false', () => {
                expect(isLoading(getState({ packagesSpace: { state: 'loaded' } }))).toBe(false);
            });
        });

        describe('getPosition', () => {
            test('should return undefined', () => {
                expect(getPosition(getState({ page: {} }))).toBe(undefined);
            });

            test('should return 2', () => {
                expect(getPosition(getState({ page: { originalNSParams: { position: 2 } } }))).toBe(2);
            });
        });
    });
});
