describe('ns.dom', function() {

    describe('ns.html2node', function() {

        it('должен заменить все <x-a> на <a>', function() {
            this.sinon.stub(ns, 'renameNode').callsFake(function() {
                return document.createElement('a');
            });

            var html = '<div>' +
                        '<a href="//yandex.ru">yandex.ru<x-a href="//yandex.com">yandex.com</x-a></a>' +
                        '<p>' +
                            '<a href="//mail.yandex.ru">mail.yandex.ru<x-a href="//mail.yandex.com">mail.yandex.com</x-a></a>' +
                        '</p>' +
                        '</div>';

            // проверяем html, чтобы понять, что вызвался rename и заменил правильные ноды
            expect(ns.html2node(html).outerHTML).to.be.equal('<div><div>' +
            '<a href="//yandex.ru">yandex.ru<a></a></a>' +
            '<p>' +
            '<a href="//mail.yandex.ru">mail.yandex.ru<a></a></a>' +
            '</p></div></div>');
        });

        it('должен вернуть null, если передали пустой html', function() {
            // проверяем html, чтобы понять, что вызвался rename и заменил правильные ноды
            expect(ns.html2node('')).to.be.equal(null);
        });

    });

    describe('ns.renameNode', function() {

        it('должен переименовать ноду', function() {
            var node = $('<x-a href="//yandex.ru" class="myClass">text<span>text</span></x-a>')[0];

            expect(ns.renameNode(node, 'a').outerHTML).to.be.equal('<a href="//yandex.ru" class="myClass">text<span>text</span></a>');
        });

    });

});
