const { ReactEntity } = require('../../../../vendors/hermione');

const elems = {};

elems.randomImageGame = new ReactEntity({ block: 'RandomImageGame' });
elems.randomImageGame.spin = new ReactEntity({ block: 'Spin' });
elems.randomImageGame.refresh = new ReactEntity({ block: 'RandomImageGame', elem: 'TryAgain' });
elems.randomImageGame.share = new ReactEntity({ block: 'RandomImageGame', elem: 'Share' });
elems.randomImageGame.footer = new ReactEntity({ block: 'RandomImageGame', elem: 'Footer' });
elems.randomImageGame.firstLink = new ReactEntity({ block: 'RandomImageGame', elem: 'FooterItem' }).nthChild(1);
elems.randomImageGame.secondLink = new ReactEntity({ block: 'RandomImageGame', elem: 'FooterItem' }).nthChild(2);
elems.feedbackDialog = new ReactEntity({ block: 'FeedbackDialog' });

module.exports = elems;
