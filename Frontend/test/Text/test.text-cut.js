var Text = require('../../Text/Text'),
    str = 'Hello, hello, hello, how low?',
    surrogatePairStr = 'Hello💚world',
    reverseSurrogatePairStr = '💚Hello';

describe('String cut/reverse/wrap methods', function() {
    describe('#cut', function() {
        it('should return same string', function() {
            Text.cut(str, 64).should.equal(str);
        });

        it('should return cut version of string (ended with ...)', function() {
            Text.cut(str, 16).should.equal('Hello, hello, he...');
        });

        it('should return cut version of string (ended with &hellip;)', function() {
            Text.cut(str, 16, true).should.equal('Hello, hello, he\u2026');
        });

        it('should return cut version of string (ended with CUSTOM_END)', function() {
            Text.cut(str, 16, ' CUSTOM_END').should.equal('Hello, hello, he CUSTOM_END');
        });

        // TODO handle length as code points, not UTF-16 code units
        it('should drop high surrogate in end when pair was split in halves', function() {
            Text.cut(surrogatePairStr, 6).should.equal('Hello...');
        });

        // TODO handle length as code points, not UTF-16 code units
        it('should not drop low surrogate in end when pair was not split in halves', function() {
            Text.cut(surrogatePairStr, 7).should.equal('Hello💚...');
        });
    });

    describe('#smartCut', function() {
        it('should return same string', function() {
            Text.smartCut(str, 64).should.equal(str);
        });

        it('should split string if it has single huge word', function() {
            Text.smartCut('Hellohellohello', 10).should.equal('Hellohello...');
        });

        // TODO handle length as code points, not UTF-16 code units
        it('should drop high surrogate in end when pair was split in halves', function() {
            Text.smartCut(surrogatePairStr, 6).should.equal('Hello...');
        });

        describe('with maxWord', function() {
            it('should return cut version of string (ended with ...)', function() {
                Text.smartCut(str, 16, 7).should.equal('Hello, hello, he...');
            });

            it('should return cut version of string (ended with &hellip;)', function() {
                Text.smartCut(str, 16, 7, true).should.equal('Hello, hello, he\u2026');
            });

            it('should return cut version of string (ended with CUSTOM_END)', function() {
                Text.smartCut(str, 16, 7, ' CUSTOM_END').should.equal('Hello, hello, he CUSTOM_END');
            });
        });

        describe('without maxWord', function() {
            it('should return cut version of string (ended with ...)', function() {
                Text.smartCut(str, 16).should.equal('Hello, hello,...');
            });

            it('should return cut version of string (ended with &hellip;)', function() {
                Text.smartCut(str, 16, null, true).should.equal('Hello, hello,\u2026');
            });

            it('should return cut version of string (ended with CUSTOM_END)', function() {
                Text.smartCut(str, 16, null, ' CUSTOM_END').should.equal('Hello, hello, CUSTOM_END');
            });
        });
    });

    describe('#reverse', function() {
        it('should return reversed version of string', function() {
            Text.reverse(str).should.equal('?wol woh ,olleh ,olleh ,olleH');
        });
    });

    describe('#reverseCut', function() {
        it('should return same string', function() {
            Text.reverseCut(str, 100).should.equal('Hello, hello, hello, how low?');
        });

        it('should return cut version of string', function() {
            Text.reverseCut(str, 16).should.equal('...hello, how low?');
        });

        // TODO handle length as code points, not UTF-16 code units
        it('should drop high surrogate in end when pair was split in halves', function() {
            Text.reverseCut(reverseSurrogatePairStr, 6).should.equal('...Hello');
        });
    });

    describe('#reverseCut', function() {
        it('should return cut version of string', function() {
            Text.reverseCut(str, 16, true).should.equal('\u2026hello, how low?');
        });
    });

    describe('#reverseCut', function() {
        it('should return cut version of string', function() {
            Text.reverseCut(str, 16, 'CUSTOM_START ').should.equal('CUSTOM_START hello, how low?');
        });
    });

    describe('#smartWrap', function() {
        it('should return formatted string', function() {
            var tstr = '<b>Load up on guns</b> <b>bring your</b> friends It\'s fun to lose and to pretend She\'s overboard and self-assured Oh, no, I know a dirty word',
                texp = '<b>Load up on guns</b> <b>bring your</b> friends<br/>It\'s fun to lose and to pretend<br/>She\'s overboard and self-assured<br/>Oh, no, I know a dirty word';

            Text.smartWrap(tstr, 37, '<br/>').should.equal(texp);
        });

        it('should return empty string', function() {
            Text.smartWrap('', 37, '<br/>').should.equal('');
        });
    });
});
