'use strict';

import moment from 'moment';

import initialState from 'spec/lib/page-mocks/models-promotion-statistics.json';

export default {
    ...initialState,
    reportsByModel: initialState.reportsByModel.map((item, index) => ({
        ...item,
        date: moment()
            .subtract(index + 1, 'day')
            .valueOf(),
    })),
};
