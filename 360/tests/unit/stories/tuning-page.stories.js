import '../noscript';
import { storiesOf, specs, describe, it, snapshot, mount } from '../.storybook/facade';
import React from 'react';
import { Provider } from 'react-redux';
import TuningPage from '../../../components/redux/components/tuning-page/tuning-page';
import { Button } from '@ps-int/ufo-rocks/lib/components/lego-components/Button';
import { Link } from '@ps-int/ufo-rocks/lib/components/lego-components/Link';
import '../../../components/redux/components/tuning-page/index.styl';

const TARIFF_100GB_1M = '100gb_1m_2015';
const TARIFF_100GB_1Y = '100gb_1y_2015';
const TARIFF_1TB_1M = '1tb_1m_2015';
const TARIFF_1TB_1Y = '1tb_1y_2015';

import { count } from 'helpers/metrika';

jest.mock('helpers/metrika');
jest.mock('helpers/operation', () => ({}));
jest.mock('@ps-int/ufo-rocks/lib/components/user', () => require('@ps-int/ufo-rocks/_mocks_/user'));

jest.mock('../../../components/redux/store/actions/payment', () => ({
    paymentPay: jest.fn()
}));

import { paymentPay } from '../../../components/redux/store/actions/payment';

jest.mock('../../../components/redux/store/actions/dialogs', () => ({
    openDialog: jest.fn()
}));

import { openDialog } from '../../../components/redux/store/actions/dialogs';

Date.now = jest.fn(() => 1482363367071);

const packageCanceled = {
    expires: 1527415459,
    removes: 1527415459,
    free: false,
    ctime: 1524823462,
    size: 107374182400,
    name: '100gb_1m_2015',
    state: null,
    sid: 'f14d9d50caf6590b47a60399341edd18',
    subscription: false,
    order: '477127127',
    title: '100 ГБ на месяц',
    id: 54
};
const packageForCancel = {
    expires: 1530121297,
    removes: 1530121297,
    free: false,
    ctime: 1524850901,
    size: 107374182400,
    name: '100gb_1m_2015',
    state: null,
    sid: 'ca348dfbff51b176b7c12ed8a0408487',
    subscription: true,
    order: '1277823287',
    title: 'Подписка 100 ГБ на месяц',
    id: 60
};

const getPrice = (price, discount) => price * (1 - (discount || 0) / 100);

const getSpaceTariffItems = (currency, discount, perpetualDiscount) => {
    const discountObject = discount ? {
        discount: Object.assign({
            percentage: discount
        }, perpetualDiscount ? {} : {
            active_until_ts: 1563018994
        })
    } : {};
    return [
        Object.assign({
            currency: currency,
            space: 107374182400,
            display_space: '100',
            display_space_units: currency === 'RUB' ? 'ГБ' : 'GB',
            periods: {
                month: {
                    product_id: TARIFF_100GB_1M,
                    price: getPrice(currency === 'RUB' ? 80 : 2, discount)
                },
                year: {
                    product_id: TARIFF_100GB_1Y,
                    price: getPrice(currency === 'RUB' ? 800 : 20, discount)
                }
            }
        }, discountObject),
        Object.assign({
            currency: currency,
            space: 1099511627776,
            display_space: '1',
            display_space_units: currency === 'RUB' ? 'ТБ' : 'TB',
            periods: {
                month: {
                    product_id: TARIFF_1TB_1M,
                    price: getPrice(currency === 'RUB' ? 200 : 10, discount)
                },
                year: {
                    product_id: TARIFF_1TB_1Y,
                    price: getPrice(currency === 'RUB' ? 2000 : 100, discount)
                }
            }
        }, discountObject)
    ];
};

export const getStore = (props = {}) => ({
    subscribe: () => { },
    dispatch: () => { },
    getState: () => (Object.assign({}, {
        page: {
            idContext: ''
        },
        config: {
            links: {
                passport: {},
                help: { disk: 'disk', connect: 'connect', diskPro: 'diskPro' },
                connectTermsOfUse: 'connectTermsOfUse',
                termsOfUse: 'termsOfUse'
            },
            urlsBase: { www: 'yandex' },
            languagesAvailable: ['ru', 'en', 'uk', 'tr'],
            skExternal: 'y2526c6ee41d2db24387b255ba0b56166'
        },
        user: {
            isWS: false,
            hasYaPlus: false,
            features: {
                disk_pro: {
                    enabled: false
                }
            }
        },
        spaceTariffs: {
            items: getSpaceTariffItems('RUB')
        },
        defaultFolders: {
            folders: {
                yateamnda: '/yateamnda/'
            }
        },
        environment: {
            session: {
                locale: 'ru',
                auth: { accountsExtra: [] },
                experiment: {
                    diskPro2019Exp300: true
                },
                region: { }
            }
        },
        space: {
            limit: 10737418240,
            used: 3123102571,
            free: 7614315669
        },
        packagesSpace: {
            state: 'loaded',
            items: [
                {
                    ctime: 0,
                    expires: null,
                    free: true,
                    id: 42,
                    name: 'initial_10gb',
                    order: null,
                    removes: null,
                    sid: 'fd4a20d05867764ed4070a37003b1201',
                    size: 10737418240,
                    state: null,
                    subscription: false,
                    title: 'За регистрацию Диска'
                }, packageCanceled, packageForCancel
            ]
        },
        orders: [],
        dialogs: {}
    }, props))
});
const getComponent = (props) => {
    const store = getStore(props);
    return (
        <Provider store={store}>
            <TuningPage />
        </Provider>
    );
};

export default storiesOf('TuningPage', module)
    .add('состояние загрузки', ({ kind, story }) => {
        const component = getComponent({ packagesSpace: { state: 'loading', items: [] } });

        specs(() => describe(kind, () => {
            snapshot(story, component);

            it('спиннер отображается', () => {
                const wrapper = mount(component);
                expect(wrapper.find('.loader-or-error').length).toBe(1);
            });
        }));
        return component;
    })
    .add('состояние ошибки', ({ kind, story }) => {
        const component = getComponent({ packagesSpace: { state: 'error', items: [] } });

        specs(() => describe(kind, () => {
            snapshot(story, component);

            it('ошибка отображается', () => {
                const wrapper = mount(component);
                expect(wrapper.find('.loader-or-error').length).toBe(1);
            });
        }));
    })
    .add('состояние по-умолчанию', ({ kind, story }) => {
        const component = getComponent();

        specs(() => describe(kind, () => {
            snapshot(story, component);

            it('фичи у тарифов отображаются', () => {
                const component = getComponent();
                const wrapper = mount(component);
                expect(wrapper.find('.features__feature').length).toBe(12);
            });

            it('покупка тарифа на год', () => {
                const component = getComponent();
                const wrapper = mount(component);

                wrapper.find('.buy-button').find(Button).at(0).simulate('click');
                expect(paymentPay).toHaveBeenLastCalledWith(TARIFF_100GB_1Y);
                expect(openDialog)
                    .toHaveBeenLastCalledWith('payment', { isYandexPlus: undefined, space: 107374182400 });
                expect(count).toHaveBeenLastCalledWith('interface elements', 'upgrade page', 'buy', TARIFF_100GB_1Y);
            });

            it('покупка тарифа на месяц', () => {
                const component = getComponent();
                const wrapper = mount(component);

                wrapper.find('.buy-link').find(Link).at(0).simulate('click');
                expect(paymentPay).toHaveBeenLastCalledWith(TARIFF_100GB_1M);
                expect(openDialog)
                    .toHaveBeenLastCalledWith('payment', { isYandexPlus: undefined, space: 107374182400 });
                expect(count).toHaveBeenLastCalledWith('interface elements', 'upgrade page', 'buy', TARIFF_100GB_1M);
            });

            it('отмена услуги', () => {
                const component = getComponent();
                const wrapper = mount(component);

                wrapper.find('.tuning-page-history__history-desc').find(Button).simulate('click');

                expect(openDialog).toHaveBeenLastCalledWith('paymentCancel', { package: packageForCancel });
                expect(count).toHaveBeenLastCalledWith('interface elements', 'upgrade page', 'cancel sub');
            });

            it('открытие попапа промокода', () => {
                const component = getComponent();
                const wrapper = mount(component);

                wrapper.find('.tuning-page-promo').find(Button).simulate('click');
                expect(count).toHaveBeenLastCalledWith('interface elements', 'upgrade page', 'promocode');
                expect(openDialog).toHaveBeenLastCalledWith('promo', {});
            });
        }));
        return component;
    })
    .add('диск про доступен', ({ kind, story }) => {
        const component = getComponent({ user: { features: { disk_pro: { enabled: true } } } });

        specs(() => describe(kind, () => {
            snapshot(story, component);

            it('фичи над тарифами отображаются', () => {
                const wrapper = mount(component);
                expect(wrapper.find('.features__feature').length).toBe(6);
            });
        }));
    })
    .add('валюта в USD', ({ kind, story }) => {
        const component = getComponent({ spaceTariffs: { items: getSpaceTariffItems('USD') } });

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));
    })
    .add('валюта в ¥', ({ kind, story }) => {
        const component = getComponent({ spaceTariffs: { items: getSpaceTariffItems('¥') } });

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));
    })
    .add('скидка 10% в RUB', ({ kind, story }) => {
        const component = getComponent({ spaceTariffs: { items: getSpaceTariffItems('RUB', 10) } });

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));
    })
    .add('скидка 20% в RUB', ({ kind, story }) => {
        const component = getComponent({ spaceTariffs: { items: getSpaceTariffItems('RUB', 20) } });

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));
    })
    .add('скидка 30% в RUB', ({ kind, story }) => {
        const component = getComponent({ spaceTariffs: { items: getSpaceTariffItems('RUB', 30) } });

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));
    })
    .add('скидка 10% в USD', ({ kind, story }) => {
        const component = getComponent({ spaceTariffs: { items: getSpaceTariffItems('USD', 10) } });

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));
    })
    .add('скидка 20% в USD', ({ kind, story }) => {
        const component = getComponent({ spaceTariffs: { items: getSpaceTariffItems('USD', 20) } });

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));
    })
    .add('скидка 30% в USD', ({ kind, story }) => {
        const component = getComponent({ spaceTariffs: { items: getSpaceTariffItems('USD', 30) } });

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));
    })
    .add('бессрочная скидка (20% в RUB)', ({ kind, story }) => {
        const component = getComponent({ spaceTariffs: { items: getSpaceTariffItems('RUB', 20, true) } });

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));
    })
    .add('разные варианты оплаченных услуг', ({ kind, story }) => {
        const props = {
            packagesSpace: {
                state: 'loaded',
                items: [
                    {
                        ctime: 0,
                        expires: null,
                        free: true,
                        id: 42,
                        name: 'music_dec_2012',
                        order: null,
                        removes: null,
                        sid: 'fd4a20d05867764ed4070a37003b1201',
                        size: 10737418240,
                        state: null,
                        subscription: false,
                        title: 'music_dec_2012'
                    }, {
                        ctime: 0,
                        expires: null,
                        free: true,
                        id: 42,
                        name: 'yandex_mail_ranktable',
                        order: null,
                        removes: null,
                        sid: 'fd4a20d05867764ed4070a37003b1201',
                        size: 10737418240,
                        state: null,
                        subscription: false,
                        title: 'yandex_mail_ranktable'
                    }, {
                        ctime: 0,
                        expires: null,
                        free: true,
                        id: 42,
                        name: 'rostelecom_2014_100gb',
                        order: null,
                        removes: null,
                        sid: 'fd4a20d05867764ed4070a37003b1201',
                        size: 10737418240,
                        state: null,
                        subscription: false,
                        title: 'rostelecom_2014_100gb'
                    }, {
                        ctime: 0,
                        expires: null,
                        free: true,
                        id: 42,
                        name: 'yandex_plus_10gb',
                        order: null,
                        removes: null,
                        sid: 'fd4a20d05867764ed4070a37003b1201',
                        size: 10737418240,
                        state: null,
                        subscription: false,
                        title: 'yandex_plus_10gb'
                    }, {
                        ctime: 0,
                        expires: null,
                        free: true,
                        id: 42,
                        name: 'ny_2014',
                        order: null,
                        removes: null,
                        sid: 'fd4a20d05867764ed4070a37003b1201',
                        size: 10737418240,
                        state: null,
                        subscription: false,
                        title: 'ny_2014'
                    }, {
                        expires: 1527415458,
                        removes: 1527415458,
                        free: false,
                        ctime: 1524823461,
                        size: 107374182400,
                        name: '100gb_1m_2015',
                        state: 'pmnt_cnld',
                        sid: 'f14d9d50caf6590b47a60399341edd18',
                        subscription: false,
                        order: '477127127',
                        title: '100 ГБ на месяц',
                        id: 54
                    },
                    {
                        expires: 1527415457,
                        removes: 1527415457,
                        free: false,
                        ctime: 1524823435,
                        size: 107374182400,
                        name: '100gb_1m_2015',
                        state: 'pmnt_dld',
                        sid: 'f14d9d50caf6590b47a60399341edd18',
                        subscription: false,
                        order: '477127127',
                        title: '100 ГБ на месяц',
                        id: 54
                    }, packageCanceled, packageForCancel
                ]
            }
        };
        const component = getComponent(props);

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));
    })
    .add('заказ в процессе оплаты', ({ kind, story }) => {
        const props = {
            orders: [
                {
                    auto: true,
                    ctime: 1528903762,
                    currency: 'RUB',
                    locale: 'ru',
                    market: 'RU',
                    mtime: 1528903775,
                    number: '1870367201',
                    otype: 'buy_new',
                    payment_method: 'bankcard',
                    price: 800,
                    product: {
                        bb_pid: '100gb_1y_2015_subs',
                        pid: '100gb_1y_2015',
                        title: 'Подписка 100 ГБ на год'
                    },
                    bb_pid: '100gb_1y_2015_subs',
                    pid: '100gb_1y_2015',
                    title: 'Подписка 100 ГБ на год',
                    state: 'payment_processing',
                    uid: '4005289282',
                    v: 1528903775306067
                }
            ]
        };
        const component = getComponent(props);

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));
    })
    .add('без оплаченных услуг', ({ kind, story }) => {
        const props = {
            packagesSpace: {
                state: 'loaded',
                items: []
            }
        };
        const component = getComponent(props);

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));
    })
    .add('ws пользователь', ({ kind, story }) => {
        const props = {
            user: {
                isWS: true,
                hasYaPlus: false,
                features: {
                    disk_pro: {
                        enabled: false
                    }
                }
            }
        };
        const component = getComponent(props);

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));
    });
