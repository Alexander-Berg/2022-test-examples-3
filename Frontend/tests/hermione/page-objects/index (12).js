const pageObject = require('@yandex-int/bem-page-object');

const El = require('./Entity');

const elems = {};

elems.Teaser = new El({ block: 'Teaser' });
elems.Teaser.VideoItemThumb = new El({ block: 'VideoItem', elem: 'Thumb' });
elems.Carousel = new El({ block: 'Carousel' });

elems.HomeScreen = new El({ block: 'HomeScreen' });
elems.HomeScreen.Greeting = new El({ block: 'HomeScreen', elem: 'Greeting' });
elems.HomeScreen.Promos = new El({ block: 'HomeScreen', elem: 'Promos' });
elems.HomeScreen.Recommendations = new El({ block: 'HomeScreen', elem: 'Recommendations' });

elems.NativeUI = new El({ block: 'NativeUI' });
elems.NativeUI.Footer = new El({ block: 'NativeUI-Footer' });

module.exports = {
    loadPageObject() {
        return pageObject.create(elems);
    },
};
