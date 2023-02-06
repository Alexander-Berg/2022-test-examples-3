'use strict';

import _ from 'lodash';
import {normalize} from 'normalizr';
import updateCreationDate from 'spec/hermione/lib/helpers/updateCreationDate';
import initialState from 'spec/lib/page-mocks/notifications.json';
// @ts-expect-error(TS7016) найдено в рамках VNDFRONT-4580
import {notificationsListEntity} from 'entities/notifications';

const NUMBER_OF_GENERATED_ITEMS = 50; // Всего элементов будет 56

const createById = (id: number) => ({
    id,
    read: false,
    title: `Notification ${id}`,
    body: 'Lorem ipsum dolor sit amet',
    creationDate: '2019-06-23T09:55:00.606774Z',
    type: 'recommended',
});

const getNotifications = (count: number) => _.map(new Array(count), (value, index) => createById(index + 1));

const {result, entities} = normalize(getNotifications(NUMBER_OF_GENERATED_ITEMS), notificationsListEntity);

export default updateCreationDate(
    {
        entities: _.merge(entities, initialState.entities),
        result: _.concat(initialState.result, result),
    },
    'notifications',
    'creationDate',
);
