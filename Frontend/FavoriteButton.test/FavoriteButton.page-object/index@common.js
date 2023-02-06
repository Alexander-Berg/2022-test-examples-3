const { ReactEntity, Entity } = require('../../../../vendors/hermione');

const FavoriteButton = new ReactEntity({ block: 'FavoriteButton' });
const Favorites = new ReactEntity({ block: 'Favorites' });
const FavoritesPopup = new ReactEntity({ block: 'FavoritesPopup' });
const FavoritesIframe = new ReactEntity({ block: 'FavoritesIframe' });
FavoritesPopup.login = new ReactEntity({ block: 'FavoritesPopup', elem: 'Button' }).mods({ login: true });
const distrPopup = new Entity({ block: 'distr-popup' });

module.exports = {
    distrPopup,
    FavoriteButton,
    Favorites,
    FavoritesPopup,
    FavoritesIframe,
};
