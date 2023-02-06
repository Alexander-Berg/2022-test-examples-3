import {getLinkItem, getGroupGetter} from '../../../../utils/mocks';

// group getter
export const getCustomersGroup = getGroupGetter('common.sidebar.unified:group.customers');

// links
export const shopsReviews = getLinkItem('common.sidebar.unified:reviews', 'market-partner:html:shops-reviews:get');
export const questions = getLinkItem('common.sidebar.unified:questions', 'market-partner:html:questions:get');
export const arbiterConversations = getLinkItem(
    'common.sidebar.unified:arbiter-conversation-list',
    'market-partner:html:arbiter-conversations:get',
);
