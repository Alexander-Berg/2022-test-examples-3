const getHashes = require('./index');

describe('getHashes()', () => {
    it('should generate hashes', () => {
        const html = `
<!DOCTYPE html>
<html lang="ru">
    <head>
        <meta charset="utf-8" />
        <meta
            name="viewport"
            content="width=device-width,initial-scale=1,shrink-to-fit=no,user-scalable=no,viewport-fit=cover"
        />
        <meta name="theme-color" content="#000000" />
        <meta name="robots" content="noindex,nofollow" />
        <link rel="icon" href="/favicon.ico" />
        <link href="/static/css/main.f530a6b6.chunk.css" rel="stylesheet" />
        <style>
            body { visibility: hidden; }
        </style>
    </head>
    <body>
        <noscript>You need to enable JavaScript to run this app.</noscript>
        <div id="root"></div>
        <script>
            console.log('important script!');
        </script>
        <script src="/static/js/main.476b6822.chunk.js"></script>
    </body>
</html>
    `;

        expect(getHashes(html)).toEqual({
            scripts: [
                "'sha256-8GrLtCdMH+SStlLKH6vDvj0ikVByaagWhdasX+hKPcc='",
            ],
            styles: [
                "'sha256-l0OAVvAnhhGrFDApKn807b3Feu8oLP9Q7AlAJ2SHy7c='"
            ],
        });
    });

    it('should works with empty scripts', () => {
        const html = `
    <script></script>
    <script type="text/javascript"></script>
    <style></style>
    <style type="text/css"></style>
    `;

        expect(getHashes(html)).toEqual({
            scripts: [
                "'sha256-47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU='",
            ],
            styles: [
                "'sha256-47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU='"
            ],
        });
    });

    it('should ignore external scripts', () => {
        const html = `
    <script src="index.js"></script>
    <script type="text/javascript" src="index.js"></script>
    `;

        expect(getHashes(html)).toEqual({
            scripts: [],
            styles: [],
        });
    });

    it('should works with empty page', () => {
        const html = '';

        expect(getHashes(html)).toEqual({
            scripts: [],
            styles: [],
        });
    });
});
