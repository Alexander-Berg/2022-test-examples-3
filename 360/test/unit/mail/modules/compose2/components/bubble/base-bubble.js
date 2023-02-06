describe('Daria.BaseBubble', function() {
    describe('Конструктор', function() {
        it('если передан data-yabble-value, то данные выбираются из data-yabble-* атрибутов', function() {
            var node = document.createElement('span');
            node.setAttribute('data-yabble-value', '"test123" <test123@ya.ru>');
            node.innerText = '"test" <test@ya.ru>';

            var bubble = new Daria.BaseBubble(node);

            expect(bubble.get('value')).to.be.eql('"test123" <test123@ya.ru>');
        });
    });

    describe('#toString', function() {
        it('должен вернуть значение value', function() {
            var node = document.createElement('span');
            node.setAttribute('data-yabble-value', '"test123" <test123@ya.ru>');
            node.innerText = '"test" <test@ya.ru>';

            var bubble = new Daria.BaseBubble(node);

            expect(bubble.toString()).to.be.eql('"test123" <test123@ya.ru>');
            expect(bubble.get('value')).to.be.eql('"test123" <test123@ya.ru>');
        });
    });
});

