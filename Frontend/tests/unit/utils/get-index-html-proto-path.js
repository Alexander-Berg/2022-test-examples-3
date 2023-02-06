const getIndexHtmlProtoPath = require('../../../src/server/utils/get-index-html-proto-path');

describe('utils/getIndexHtmlProtoPath', function() {
    it('должна возврщать путь к первому html-файлу в наборе с подстрокой "index" в названии', function() {
        const paths = [
            '/var/folders/_s/z7v0t3rs5577mv805m3z2vlsb7wzc3/T/sbs-nfTu6Z/shur_1_files/remarketing0.html',
            '/var/folders/_s/z7v0t3rs5577mv805m3z2vlsb7wzc3/T/sbs-nfTu6Z/shur_1_files/index.html',
            '/var/folders/_s/z7v0t3rs5577mv805m3z2vlsb7wzc3/T/sbs-nfTu6Z/shur_1_files/remarketing2.html',
        ];

        assert.equal(getIndexHtmlProtoPath(paths), paths[1]);
    });

    it('должна возврщать путь к html-файлу на меньшем уровне вложенности #0', function() {
        const paths = [
            '/var/folders/_s/z7v0t3rs5577mv805m3z2vlsb7wzc3/T/sbs-nfTu6Z/shur_1_files/index.html',
            '/var/folders/_s/z7v0t3rs5577mv805m3z2vlsb7wzc3/T/sbs-nfTu6Z/shur_1_files/remarketing.html',
            '/var/folders/_s/z7v0t3rs5577mv805m3z2vlsb7wzc3/T/sbs-nfTu6Z/index.html',
            '/var/folders/_s/z7v0t3rs5577mv805m3z2vlsb7wzc3/T/sbs-nfTu6Z/shur_1.html',
        ];

        assert.equal(getIndexHtmlProtoPath(paths), paths[2]);
    });

    it('должна возврщать путь к html-файлу на меньшем уровне вложенности #1', function() {
        const paths = [
            '/var/folders/_s/z7v0t3rs5577mv805m3z2vlsb7wzc3/T/sbs-nfTu6Z/shur_1_files/index.html',
            '/var/folders/_s/z7v0t3rs5577mv805m3z2vlsb7wzc3/T/sbs-nfTu6Z/shur_1_files/remarketing.html',
            '/var/folders/_s/z7v0t3rs5577mv805m3z2vlsb7wzc3/T/sbs-nfTu6Z/shur_1.html',
        ];

        assert.equal(getIndexHtmlProtoPath(paths), paths[2]);
    });

    it('должна возврщать путь к первому html-файлу в наборе, если ни один файл не имеет в названии подстроки "index"', function() {
        const paths = [
            '/var/folders/_s/z7v0t3rs5577mv805m3z2vlsb7wzc3/T/sbs-nfTu6Z/shur_1_files/remarketing0.html',
            '/var/folders/_s/z7v0t3rs5577mv805m3z2vlsb7wzc3/T/sbs-nfTu6Z/shur_1_files/remarketing1.html',
            '/var/folders/_s/z7v0t3rs5577mv805m3z2vlsb7wzc3/T/sbs-nfTu6Z/shur_1_files/remarketing2.html',
        ];

        assert.equal(getIndexHtmlProtoPath(paths), paths[0]);
    });

    it('должна возвращать пустую строку, если аргументом передано некорректное значение', function() {
        assert.equal(getIndexHtmlProtoPath([]), '');
        assert.equal(getIndexHtmlProtoPath('st'), '');
        assert.equal(getIndexHtmlProtoPath(), '');
    });
});
