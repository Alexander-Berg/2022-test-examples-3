import {IGenericOrderInfo} from 'server/api/GenericOrderApi/types/common/IGenericOrderInfo';
import EServiceType from 'server/api/GenericOrderApi/types/common/EServiceType';

import getECommerceRevenue from '../getECommerceRevenue';

describe('getECommerceRevenue(orderDetails)', () => {
    it('Должен вернуть revenue равный 55 для одного пассажира с одним билетом', () => {
        const orderInfo = {
            services: [
                {
                    serviceType: EServiceType.TRAIN,
                    trainInfo: {
                        passengers: [
                            {
                                ticket: {
                                    amount: {value: 1000},
                                    payment: {fee: {value: 100}},
                                },
                            },
                        ],
                    },
                },
            ],
        };

        expect(getECommerceRevenue(orderInfo as IGenericOrderInfo)).toBe(55);
    });
    it('Должен вернуть revenue равный 110 для двух поездов и одного пассажира', () => {
        const orderInfo = {
            services: [
                {
                    serviceType: EServiceType.TRAIN,
                    trainInfo: {
                        passengers: [
                            {
                                ticket: {
                                    amount: {value: 1000},
                                    payment: {fee: {value: 100}},
                                },
                            },
                        ],
                    },
                },
                {
                    serviceType: EServiceType.TRAIN,
                    trainInfo: {
                        passengers: [
                            {
                                ticket: {
                                    amount: {value: 1000},
                                    payment: {fee: {value: 100}},
                                },
                            },
                        ],
                    },
                },
            ],
        };

        expect(getECommerceRevenue(orderInfo as IGenericOrderInfo)).toBe(110);
    });

    it('Должен вернуть revenue равный 93 для двух пассажиров, у которых по одному билету', () => {
        const orderInfo = {
            services: [
                {
                    serviceType: EServiceType.TRAIN,
                    trainInfo: {
                        passengers: [
                            {
                                ticket: {
                                    amount: {value: 1000},
                                    payment: {fee: {value: 100}},
                                },
                            },
                            {
                                ticket: {
                                    amount: {value: 800},
                                    payment: {fee: {value: 80}},
                                },
                            },
                        ],
                    },
                },
            ],
        };

        expect(getECommerceRevenue(orderInfo as IGenericOrderInfo)).toBe(93);
    });

    it('Должен вернуть округленный revenue равный 174.38 для двух пассажиров, у которых по одному билету', () => {
        const orderInfo = {
            services: [
                {
                    serviceType: EServiceType.TRAIN,
                    trainInfo: {
                        passengers: [
                            {
                                ticket: {
                                    amount: {value: 876.98},
                                    payment: {fee: {value: 112.59}},
                                },
                            },
                            {
                                ticket: {
                                    amount: {value: 1326.27},
                                    payment: {fee: {value: 154.84}},
                                },
                            },
                        ],
                    },
                },
            ],
        };

        expect(getECommerceRevenue(orderInfo as IGenericOrderInfo)).toBe(
            174.38,
        );
    });
});
