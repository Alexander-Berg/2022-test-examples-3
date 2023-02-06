var Resizer = require('../../Resizer/Resizer'),
    assert = require('assert');

// загружаем руками, так как борщик в тестах не отработает

describe('Resizer метод createToken', function() {
    var resizer = null;

    beforeEach(function() {
        resizer = Resizer('//resize.yandex.net/');
    });

    it('формирует правильный url', function() {
        assert.strictEqual(
            resizer.createToken(
                '//avatars.yandex.net/get-afisha-gallery/4d3cabe115065bd43f0e43b7f837e018/orig',
                100,
                150,
                {
                    crop: 1,
                    color: 123
                }
            ),
            '//resize.yandex.net/si?key=11ee34b8469830522d7e2853e3e810c1' +
                '&url=http%3A%2F%2Favatars.yandex.net%2Fget-afisha-gallery' +
                '%2F4d3cabe115065bd43f0e43b7f837e018%2Forig&width=100&height=150&typemap=gif' +
                '%3Agif%3Bpng%3Apng%3B*%3Ajpeg%3B&crop=yes&enlarge=0&goldenratio=yes'
        );
    });

    it('формирует правильный url с большим количеством параметров', function() {
        assert.strictEqual(
            resizer.createToken(
                'http://avatars.yandex.net/get-afisha-gallery/b9ae5c7a6fec2224f4f8eac4b478b971/orig',
                100,
                150,
                {
                    crop: 1,
                    goldenratio: 1
                }
            ),
            '//resize.yandex.net/si?key=dc2211e41292a2e234fb377d88f88da1' +
                '&url=http%3A%2F%2Favatars.yandex.net%2Fget-afisha-gallery%2Fb' +
                '9ae5c7a6fec2224f4f8eac4b478b971%2Forig&width=100&height=150&typemap=gif%3A' +
                'gif%3Bpng%3Apng%3B*%3Ajpeg%3B&crop=yes&enlarge=0&goldenratio=yes'
        );
    });

    it('формирует правильный url с typemap-ом', function() {
        assert.strictEqual(
            resizer.createToken(
                'https://cdn.scratch.mit.edu/static/site/users/avatars/525/9700.png',
                640,
                1136,
                'gif:gif;png:png;*:tiff;'
            ),
            '//resize.yandex.net/si?key=add41e5e40c69c8edb5fb3667d196364' +
                '&url=https%3A%2F%2Fcdn.scratch.mit.edu%2Fstatic%2Fsite%2Fusers%2Favatars%2F' +
                '525%2F9700.png&width=640&height=1136&typemap=gif%3Agif%3Bpng%3Apng%3B*%3Atiff%3B&' +
                'crop=no&enlarge=0&goldenratio=yes'
        );
    });

    it('формирует правильный url без доп. параметров', function() {
        assert.strictEqual(
            resizer.createToken(
                'http://media.realitatea.ro/multimedia/image/200902/w728/image_123453967331635000_1.jpg',
                320,
                568
            ),
            '//resize.yandex.net/si?key=83e51f451a3230d4c64a2dd5afdfb49b' +
                '&url=http%3A%2F%2Fmedia.realitatea.ro%2Fmultimedia%2Fimage%2F200902' +
                '%2Fw728%2Fimage_123453967331635000_1.jpg&width=320&height=568&typemap=gif%3Agif%3B' +
                'png%3Apng%3B*%3Ajpeg%3B&crop=no&enlarge=0&goldenratio=yes'
        );
    });

    it('формирует правильный url с нулевой шириной', function() {
        assert.strictEqual(
            resizer.createToken(
                'http://mastistudio.com/wall_images/wall_image_1687.jpg',
                640,
                0,
                'png:png;*:jpg'
            ),
            '//resize.yandex.net/si?key=93eb1622e12f4a25b4308fb52c0f5ce6' +
            '&url=http%3A%2F%2Fmastistudio.com%2Fwall_images%2Fwall_image_1687.jpg' +
            '&width=640&height=0&typemap=png%3Apng%3B*%3Ajpg&crop=no&enlarge=0&goldenratio=yes'
        );
    });

    it('бросает ошибку, если нет хотя бы одного из параметров url, width или height', function() {
        assert.throws(function() {
            resizer.createToken(null, 100, 100);
        });
        assert.throws(function() {
            resizer.createToken('http://i.allday.ru/uploads/posts/2009-07/thumbs/1247333322_1.jpg', null, 100);
        });
        assert.throws(function() {
            resizer.createToken('http://i.allday.ru/uploads/posts/2009-07/thumbs/1247333322_1.jpg', 100);
        });
    });
});
