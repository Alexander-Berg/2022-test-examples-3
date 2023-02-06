import { ReasonToBuyId } from '@yandex-turbo/applications/market.yandex.ru/interfaces';

export const defaultData = {
    viewedReason: {
        id: ReasonToBuyId.VIEWED_N_TIMES,
        value: 2,
    },
    boughtReason: {
        id: ReasonToBuyId.BOUGHT_N_TIMES,
        value: 2,
    },
};
