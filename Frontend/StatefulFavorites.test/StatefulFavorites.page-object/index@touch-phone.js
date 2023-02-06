const { ReactEntity } = require('../../../../vendors/hermione');

const elems = {};

elems.statefulFavorites = new ReactEntity({ block: 'StatefulFavorites' });
elems.statefulFavoritesTitle = new ReactEntity({ block: 'StatefulFavorites', elem: 'Title' });
elems.statefulFavorites.scroller = new ReactEntity({ block: 'Scroller' });
elems.statefulFavorites.scroller.firstCard = new ReactEntity({ block: 'StatefulFavoriteCard' }).nthChild(1);
elems.statefulFavorites.scroller.firstCard.link = new ReactEntity({ block: 'Link' });
elems.statefulFavoritesDrawer = new ReactEntity({ block: 'StatefulFavorites', elem: 'Drawer' });
elems.statefulFavoritesDrawer.Content = new ReactEntity({ block: 'Drawer', elem: 'Content' });
elems.statefulFavoritesDrawer.Content.firstCard = new ReactEntity({ block: 'StatefulFavoriteCard' }).nthChild(1);
elems.statefulFavoritesDrawer.Content.firstCard.link = new ReactEntity({ block: 'Link' });
elems.statefulFavoritesDrawer.Content.button = new ReactEntity({ block: 'Button2' });
elems.statefulFavoritesDrawer.Content.spin = new ReactEntity({ block: 'Spin' });
elems.statefulFavoritesDrawer.Feed = new ReactEntity({ block: 'StatefulFavoritesFeed' });
elems.statefulFavoritesDrawer.Feed.Image = new ReactEntity({ block: 'Image' });

module.exports = elems;
