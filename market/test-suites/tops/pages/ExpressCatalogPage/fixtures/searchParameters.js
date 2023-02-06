import {FILTER_IDS} from '@self/root/src/constants/filters';
import {EXPRESS_FILTER_ON} from '@self/root/src/helpers/expressDelivery';

/**
 * Хак вместе мока @self/root/src/widgets/content/SearchParameters
 *
 * Сделать честный мок показалось дорогим
 */
export const searchParameters = {
    searchContext: 'express',
    [FILTER_IDS.EXPRESS]: EXPRESS_FILTER_ON,
};
