var http = require('http');

/**
 * Функция проверяет доступность урлов, делая запрос и проверяя код ответа
 * TODO https://st.yandex-team.ru/FEI-9364
 *
 * @param {String[]} urls - список урлов
 * @param {Function} done - колбек завершения теста
 * @param {String} errMsg - сообщение об ошибке
 */
function checkUrls(urls, done, errMsg) {
    var counter = 1,
        statusCodes = [],
        urlCount = urls.length,
        HTTP_OK = 200;

    urls.forEach(url => {
        http.get(url, function(res) {
            res.on('data', function(data) { });
            res.on('end', function() {
                statusCodes.push(res.statusCode);

                counter++;
                if (counter === urlCount) {
                    statusCodes.forEach((status, index) => {
                        assert.equal(status, HTTP_OK, index + errMsg);
                    });

                    done();
                }
            });
        });
    });
}

describeBlock('i-mds-image__avatar-url', function(block) {
    let item, format;

    beforeEach(function() {
        item = { image: {} };
        format = 'Ux150';
    });

    /**
     * TODO https://st.yandex-team.ru/FEI-9364
     *
     * @param {Object[]} imageParams
     *
     * @returns {String[]}
     */
    function createImgUrls(imageParams) {
        return imageParams.map(params => {
            item.image.mds_avatar_id = params.mds_avatar_id;

            return 'http:' + block(item, params.format);
        });
    }

    it('should return empty string if image has no data', function() {
        assert.strictEqual(block(item, format), '');
    });

    it('should build image URL with avatar ID', function() {
        item.image.avatar = '//ya.ru/image.png';
        item.image.mds_avatar_id = '65260/77343673';

        assert.strictEqual(block(item, format), '//avatars.mds.yandex.net/get-entity_search/65260/77343673/SUx150');
    });

    it('should return avatar URL if avatar ID does not exists', function() {
        item.image.avatar = '//ya.ru/image.png';

        assert.strictEqual(block(item, format), '//ya.ru/image.png');
    });

    // TODO https://st.yandex-team.ru/FEI-9364
    it.skip('should return correctly url for entity badge images', function(done) {
        var images = [
            {
                // Мадонна (gallery)
                mds_avatar_id: '761788/293386332',
                format: 'Ux182_2x'
            }, {
                // Мадонна (gallery/touch)
                mds_avatar_id: '761788/293386332',
                format: 'Ux150_2x'
            }, {
                // Париж (map)
                mds_avatar_id: '51460/234163984',
                format: 'Ux182_2x'
            }, {
                // Кейт миддлтон (face)
                mds_avatar_id: '478647/293605033',
                format: '122x183Top_2x'
            }, {
                // Кейт миддлтон (face/pad)
                mds_avatar_id: '478647/293605033',
                format: '122x183Top_2x'
            }, {
                // Кейт миддлтон (face/touch)
                mds_avatar_id: '478647/293605033',
                format: '91x121Top_2x'
            }, {
                // боевые ботаники (logoFit)
                mds_avatar_id: '767653/282797127',
                format: '122x122Fit_2x'
            }, {
                // боевые ботаники (logoFit/pad)
                mds_avatar_id: '767653/282797127',
                format: '122x122Fit_2x'
            }, {
                // боевые ботаники (logoFit/touch)
                mds_avatar_id: '767653/282797127',
                format: '122x122FitScale_2x'
            }, {
                // флаг Монако (logoFit)
                mds_avatar_id: '17809/125226702',
                format: '122x122Fit_2x'
            }, {
                // си-эн тауэр (VertCrop)
                mds_avatar_id: '99532/241880035',
                format: '90x168_2x'
            }, {
                // си-эн тауэр (VertCrop/pad)
                mds_avatar_id: '99532/241880035',
                format: '100x184_2x'
            }, {
                // Телевизионная башня Токио (VertFit)
                mds_avatar_id: '96437/237975829',
                format: 'Ux168_2x'
            }, {
                // Телевизионная башня Токио (VertFit/pad)
                mds_avatar_id: '96437/237975829',
                format: 'Ux184_2x'
            }, {
                // Телевизионная башня Токио (VertFit/touch)
                mds_avatar_id: '96437/237975829',
                format: 'Ux120_2x'
            }, {
                // Остров Сите (VertSquare)
                mds_avatar_id: '140166/250623902',
                format: '122x122_2x'
            }, {
                // Остров Сите (HorizCrop)
                mds_avatar_id: '140166/250623902',
                format: '168x90_2x'
            }, {
                // Остров Сите (HorizCrop/pad)
                mds_avatar_id: '140166/250623902',
                format: '184x100_2x'
            }, {
                // Остров Сите (HorizCrop/touch)
                mds_avatar_id: '140166/250623902',
                format: 'Ux150_2x'
            }, {
                // Остров Сите (HorizFit)
                mds_avatar_id: '140166/250623902',
                format: '168xU_2x'
            }, {
                // Остров Сите (HorizSquare)
                mds_avatar_id: '140166/250623902',
                format: '134x134_2x'
            }
        ];

        checkUrls(createImgUrls(images), done, ' картинка бейджа ОО содержит ошибку');
    });

    // TODO https://st.yandex-team.ru/FEI-9364
    it.skip('should return correctly url for entity fact images', function(done) {
        var images = [
            {
                // Cколько лет Жириновскому (face)
                mds_avatar_id: '922086/290998786',
                format: '122x162Face'
            }, {
                // Cколько лет Жириновскому (face/pad)
                mds_avatar_id: '922086/290998786',
                format: '134x178Face'
            }, {
                // Cколько лет Жириновскому (face/touch)
                mds_avatar_id: '922086/290998786',
                format: '114x152Face'
            }, {
                // Кто написал война и мир (vertical_book)
                mds_avatar_id: '122335/158478135',
                format: '122x162'
            }, {
                // Кто написал война и мир (vertical_book/pad)
                mds_avatar_id: '122335/158478135',
                format: '134x178'
            }, {
                // Кто написал война и мир (vertical_book/touch)
                mds_avatar_id: '122335/158478135',
                format: '114x152'
            }, {
                // Сколько калорий в арахисе (VertSquare)
                mds_avatar_id: '46786/212263628',
                format: '168x168'
            }, {
                // Сколько калорий в арахисе (VertSquare/touch)
                mds_avatar_id: '46786/212263628',
                format: '91x91'
            }, {
                // Сколько калорий в арахисе (HorizCrop/touch)
                mds_avatar_id: '46786/212263628',
                format: '168x90'
            }
        ];

        checkUrls(createImgUrls(images), done, ' картинка фактовой карточки содержит ошибку');
    });

    // TODO https://st.yandex-team.ru/FEI-9364
    it.skip('should return correctly url for entity carousel images', function(done) {
        var images = [
            {
                // Мадонна > смотрите также
                mds_avatar_id: '793860/293419954',
                format: '122x162Face_2x'
            }, {
                // Николай Васильевич Гоголь > Книги
                mds_avatar_id: '28506/154252302',
                format: '122x162_2x'
            }, {
                // Николай Васильевич Гоголь > Книги (pad)
                mds_avatar_id: '28506/154252302',
                format: '134x178_2x'
            }, {
                // Николай Васильевич Гоголь > Книги (touch)
                mds_avatar_id: '28506/154252302',
                format: '114x171_2x'
            }, {
                // Audi Q3 > Смотрите также
                mds_avatar_id: '192978/233261225',
                format: '168x126_2x'
            }, {
                // Audi Q3 > Смотрите также (pad)
                mds_avatar_id: '192978/233261225',
                format: '184x138_2x'
            }, {
                // Audi Q3 > Смотрите также (touch)
                mds_avatar_id: '192978/233261225',
                format: '114x86_2x'
            }, {
                // кино 2018
                mds_avatar_id: '371114/283448649',
                format: '168x252_2x'
            }, {
                // кино 2018 (pad)
                mds_avatar_id: '371114/283448649',
                format: '134x201_2x'
            }, {
                // Франция > Смотрите также
                mds_avatar_id: '67347/127897985',
                format: '122x91_2x'
            }, {
                // Франция > Смотрите также (pad)
                mds_avatar_id: '67347/127897985',
                format: '134x100_2x'
            }, {
                // Первый канал > Смотрите также
                mds_avatar_id: '122335/134033065',
                format: '122x122FitScale_2x'
            }, {
                // Первый канал > Смотрите также (pad)
                mds_avatar_id: '122335/134033065',
                format: '134x134FitScale_2x'
            }, {
                // Первый канал > Смотрите также (touch)
                mds_avatar_id: '122335/134033065',
                format: '114x114FitScale_2x'
            }
        ];

        checkUrls(createImgUrls(images), done, ' картинка карусели ОО содержит ошибку');
    });

    // TODO https://st.yandex-team.ru/FEI-9364
    it.skip('should return correctly url for entity list images', function(done) {
        var images = [
            {
                // Мадонна список "Смотрите также"
                mds_avatar_id: '517208/293631496',
                format: '76x101Face_2x'
            }, {
                // Мадонна список "Смотрите также" (pad)
                mds_avatar_id: '517208/293631496',
                format: '84x112Face_2x'
            }, {
                // Николай Васильевич Гоголь > Книги
                mds_avatar_id: '68218/111330289',
                format: '76x101_2x'
            }, {
                // Николай Васильевич Гоголь > Книги (pad)
                mds_avatar_id: '68218/111330289',
                format: '84x112_2x'
            }, {
                // Audi Q3 список "Смотрите также"
                mds_avatar_id: '192978/233261225',
                format: '76x57_2x'
            }, {
                // Audi Q3 список "Смотрите также" (pad)
                mds_avatar_id: '192978/233261225',
                format: '84x63_2x'
            }, {
                // излом времени список "Смотрите также" //avatars.mds.yandex.net/get-entity_search/
                mds_avatar_id: '122335/125610412',
                format: '76x114_2x'
            }, {
                // излом времени список "Смотрите также" (pad)
                mds_avatar_id: '122335/125610412',
                format: '84x126_2x'
            }, {
                // Франция список "Смотрите также"
                mds_avatar_id: '67347/127897985',
                format: '122x91_2x'
            }, {
                // Франция список "Смотрите также" (pad)
                mds_avatar_id: '67347/127897985',
                format: '134x100_2x'
            }, {
                // Первый канал список "Смотрите также"
                mds_avatar_id: '122335/134033065',
                format: '76x76FitScale_2x'
            }, {
                // Первый канал список "Смотрите также" (pad)
                mds_avatar_id: '122335/134033065',
                format: '84x84FitScale_2x'
            }, {
                // Первый канал список "Смотрите также"
                mds_avatar_id: '135316/99968864',
                format: '76x76Smart_2x'
            }, {
                // Первый канал список "Смотрите также" (pad)
                mds_avatar_id: '135316/99968864',
                format: '84x84Smart_2x'
            }
        ];

        checkUrls(createImgUrls(images), done, ' картинка списка ОО содержит ошибку');
    });
});

describeBlock('i-mds-image__get-size', function(block) {
    var item;

    beforeEach(function() {
        item = { image: { original_size: {} } };
    });

    it('should return width and height for static `100x100` format ', function() {
        var size = block(item, '100x100');

        assert.equal(size.width, 100);
        assert.equal(size.height, 100);
    });

    it('should calculate width for `Ux150` format ', function() {
        item.image.original_size.width = 1000;
        item.image.original_size.height = 1500;
        var size = block(item, 'Ux150');

        assert.equal(size.width, 100);
        assert.equal(size.height, 150);
    });

    it('should calculate height for `120xU` format ', function() {
        item.image.original_size.width = 1200;
        item.image.original_size.height = 1000;
        var size = block(item, '120xU');

        assert.equal(size.width, 120);
        assert.equal(size.height, 100);
    });
});

describeBlock('i-mds-image__format-type-by-ratio', function(block) {
    var format;

    beforeEach(function() {
        format = 'face';
    });

    it('should return correct format depending of ratio', function() {
        assert.equal(block(0.01, format), 'vert_crop');
        assert.equal(block(0.52, format), 'vert_crop');

        assert.equal(block(0.53, format), 'vert_fit');
        assert.equal(block(0.74, format), 'vert_fit');

        assert.equal(block(0.75, format), 'vert_square');
        assert.equal(block(1.14, format), 'vert_square');

        assert.equal(block(1.15, format), 'horiz_square');
        assert.equal(block(1.32, format), 'horiz_square');

        assert.equal(block(1.33, format), 'horiz_fit');
        assert.equal(block(1.86, format), 'horiz_fit');

        assert.equal(block(1.87, format), 'horiz_crop');
        assert.equal(block(2.00, format), 'horiz_crop');
    });

    it('should return "Logo" format for "square_fit" and P >= 2', function() {
        format = 'square_fit';
        assert.equal(block(2.00, format), 'logo');
    });
});
