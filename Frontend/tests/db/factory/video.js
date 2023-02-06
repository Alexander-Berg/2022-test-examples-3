const BaseFactory = require('tests/db/factory/base');

class VideoFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            source: 'youtube',
            iframeUrl: 'https://www.youtube.com/embed/zB4I68XVPzQ',
            videoUrl: 'https://youtu.be/zB4I68XVPzQ',
            videoId: 'zB4I68XVPzQ',
            title: 'Star Wars: The Last Jedi Official Teaser',
            duration: 92,
            definition: 'hd',
            thumbnail: 'https://i.ytimg.com/vi/zB4I68XVPzQ/hqdefault.jpg',
            thumbnailHeight: 360,
            thumbnailWidth: 480,
        };
    }

    static get table() {
        return require('db').video;
    }

    static get subFactories() {
        return {
            programItemId: require('tests/db/factory/programItem'),
        };
    }
}

module.exports = VideoFactory;
