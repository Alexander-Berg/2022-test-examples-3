describe('daria.extensions', function() {

    describe('Daria.splitName', function() {

        it('"file.txt" -> ["file", "txt"]', function() {
            expect(Daria.splitName('file.txt')).to.eql(['file', 'txt']);
        });

        it('"file" -> ["file", ""]', function() {
            expect(Daria.splitName('file')).to.eql(['file', '']);
        });

        it('"file.txt.txt" -> ["file.txt", "txt"]', function() {
            expect(Daria.splitName('file.txt.txt')).to.eql(['file.txt', 'txt']);
        });
    });

    describe('Daria.getExtension', function() {

        it('"file.txt" -> "txt"', function() {
            expect(Daria.getExtension('file.txt')).to.eql('txt');
        });

        it('"file" -> ""', function() {
            expect(Daria.getExtension('file')).to.eql('');
        });

        it('undefined -> ""', function() {
            expect(Daria.getExtension()).to.eql('');
        });
    });
});

