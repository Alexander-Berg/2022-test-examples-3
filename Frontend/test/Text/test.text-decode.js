var Text = require('../../Text/Text');

describe('#decodeHtmlEntities', function() {
    it('should decode named entities', function() {
        Text.decodeHtmlEntities('&deg;').should.equal('°');
        Text.decodeHtmlEntities('&beta;').should.equal('β');
        Text.decodeHtmlEntities('&cent;').should.equal('¢');
    });

    it('should decode entities with decimal number', function() {
        Text.decodeHtmlEntities('&#176;').should.equal('°');
        Text.decodeHtmlEntities('&#946;').should.equal('β');
        Text.decodeHtmlEntities('&#162;').should.equal('¢');
    });

    it('should decode entities with hexadecimal number', function() {
        Text.decodeHtmlEntities('&#x000B0;').should.equal('°');
        Text.decodeHtmlEntities('&#x003B2;').should.equal('β');
        Text.decodeHtmlEntities('&#x000A2;').should.equal('¢');
    });

    it('should match case for named entities', function() {
        Text.decodeHtmlEntities('&beta;').should.equal('β');
        Text.decodeHtmlEntities('&Beta;').should.equal('Β');
    });

    it('should fallback to lowercase for named entities', function() {
        Text.decodeHtmlEntities('&AMP;').should.equal('&');
    });

    it('should not match case for entities with hexadecimal number', function() {
        Text.decodeHtmlEntities('&#x003B2;').should.equal('β');
        Text.decodeHtmlEntities('&#x003b2;').should.equal('β');
    });

    it('should decode all entries', function() {
        Text.decodeHtmlEntities('&deg;&#946;&#x000A2;').should.equal('°β¢');
    });
});
