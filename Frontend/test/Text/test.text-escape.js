var Text = require('../../Text/Text');

describe('Escaping methods', function() {
    describe('#uriEscape', function() {
        it('should return string if number given', function() {
            Text.uriEscape(100).should.equal('100');
        });

        it('should return escaped string; char to escape: "("', function() {
            Text.uriEscape('( (').should.equal('%28%20%28');
        });

        it('should return escaped string; char to escape: ")"', function() {
            Text.uriEscape('))').should.equal('%29%29');
        });

        it('should return escaped string; char to escape: "\'"', function() {
            Text.uriEscape('\'\'').should.equal('%27%27');
        });

        it('should return escaped string; char to escape: "!"', function() {
            Text.uriEscape('! !').should.equal('%21%20%21');
        });

        it('should replace spaces by pluses if flag given', function() {
            Text.uriEscape('cat dog pig', true).should.equal('cat+dog+pig');
        });
    });

    describe('#xmlEscape', function() {
        it('should return escaped string; char to escape: "&"', function() {
            Text.xmlEscape('&&').should.equal('&amp;&amp;');
        });

        it('should return escaped string; char to escape: ">"', function() {
            Text.xmlEscape('>>').should.equal('&gt;&gt;');
        });

        it('should return escaped string; char to escape: "<"', function() {
            Text.xmlEscape('<<').should.equal('&lt;&lt;');
        });
    });

    describe('#jsAttrEscape', function() {
        it('should return same string', function() {
            Text.jsAttrEscape('&quot; &apos;', true).should.equal('&quot; &apos;');
        });

        it('should return escaped string; specialchar to escape: "&quot;"', function() {
            Text.jsAttrEscape('&quot;&quot;').should.equal('\\&quot;\\&quot;');
        });

        it('should return escaped string; specialchar to escape: "&apos;"', function() {
            Text.jsAttrEscape('&apos;&apos;').should.equal('\\&apos;\\&apos;');
        });

        it('should return escaped string; specialchar to escape: "&#034;"', function() {
            Text.jsAttrEscape('&#034;&#034;').should.equal('\\&#034;\\&#034;');
        });

        it('should return escaped string; specialchar to escape: "&#x0022;"', function() {
            Text.jsAttrEscape('&#x0022;&#x0022;').should.equal('\\&#x0022;\\&#x0022;');
        });

        it('should return escaped string; specialchar to escape: "&#039;"', function() {
            Text.jsAttrEscape('&#039;&#039;').should.equal('\\&#039;\\&#039;');
        });

        it('should return escaped string; specialchar to escape: "&#x0027;"', function() {
            Text.jsAttrEscape('&#x0027;&#x0027;').should.equal('\\&#x0027;\\&#x0027;');
        });

        it('should return escaped string; char to escape: "\u0027"', function() {
            Text.jsAttrEscape('\'\'').should.equal('\\u0027\\u0027');
        });

        it('should return escaped string; char to escape: "\u0022"', function() {
            Text.jsAttrEscape('""').should.equal('\\u0022\\u0022');
        });

        it('should return escaped string; char to escape: "\u002f"', function() {
            Text.jsAttrEscape('//').should.equal('\\u002f\\u002f');
        });

        it('should return escaped string; char to escape: "\u003c"', function() {
            Text.jsAttrEscape('<<').should.equal('\\u003c\\u003c');
        });

        it('should return escaped string; char to escape: "\u003e"', function() {
            Text.jsAttrEscape('>>').should.equal('\\u003e\\u003e');
        });

        it('should return escaped string; char to escape: "\u0021"', function() {
            Text.jsAttrEscape('!!').should.equal('\\u0021\\u0021');
        });

        it('should return escaped string; char to escape: "\u002d"', function() {
            Text.jsAttrEscape('--').should.equal('\\u002d\\u002d');
        });

        it('should return escaped string; char to escape: "\\n"', function() {
            Text.jsAttrEscape('\n\n').should.equal('\\n\\n');
        });

        it('should return escaped string; char to escape: "\\x02"', function() {
            Text.jsAttrEscape('\x02\x02').should.equal('  ');
        });

        it('should return escaped string; char to escape: "\\x20"', function() {
            Text.jsAttrEscape('\x20\x20').should.equal('  ');
        });
    });

    describe('#jsonEscape', function() {
        it('should return escaped string; char to escape: "<"', function() {
            Text.jsonEscape('<<').should.equal('\\u003c\\u003c');
        });
    });
});
