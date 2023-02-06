/**
 * БЭМ блок для рекурсивного обхода DOM-дерева.
 * Триггерит события в публичный канал 'hermione-dom-walker'
 *
 * @fires hermione-dom-walker#attribute
 * @fires hermione-dom-walker#text
 * @fires hermione-dom-walker#comment
 * @fires hermione-dom-walker#node
 */

(function(BEM, $) {
    'use strict';

    var channel = BEM.channel('hermione-dom-walker');

    BEM.decl('hermione-dom-walker', {

        /**
         * Запускает рекурсивных обход Html документа
         *
         * @property {Array<Node>|Node} node - нода (или массив) для обхода
         */
        walk: function(node) {
            this._count = 0;

            (node.length ? $.makeArray(node) : [node])
                .forEach(this._walk, this);

            channel.trigger('finished', { count: this._count });
        },

        _walk: function(node) {
            var nodeType = node.nodeType;

            if (
                nodeType !== Node.ELEMENT_NODE &&
                nodeType !== Node.TEXT_NODE &&
                nodeType !== Node.COMMENT_NODE
            ) {
                return;
            }

            this._count++;

            $.makeArray(node.childNodes)
                .forEach(this._walk, this);

            // события триггерятся при обходе от листьев дерева к корню
            switch (nodeType) {
                case Node.ELEMENT_NODE:
                    channel.trigger('node', {
                        node: node,
                        tagName: node.tagName.toLowerCase(),
                        source: sourceCallback(node, node.outerHTML)
                    });

                    $.makeArray(node.attributes).forEach(function(attribute) {
                        channel.trigger('attribute', {
                            node: node,
                            attr: attribute,
                            source: sourceCallback(node)
                        });
                    }, this);
                    break;
                case Node.TEXT_NODE:
                    channel.trigger('text', {
                        node: node.parentNode,
                        text: node.data,
                        source: sourceCallback(node.parentNode, node.data)
                    });
                    break;
                case Node.COMMENT_NODE:
                    channel.trigger('comment', {
                        node: node.parentNode,
                        text: node.data,
                        source: sourceCallback(node.parentNode, '<!--' + node.data + '-->')
                    });
                    break;
            }
        }
    }, {
        stringifyNode: stringifyNode
    });

    function stringifyNode(node, content) {
        if (node.nodeType !== Node.ELEMENT_NODE) { return '' }

        var tagName = node.tagName.toLowerCase(),
            text = (content || node.innerHTML).trim(),
            attrs = stringifyAttrs(node),
            shortenedText = text.substr(0, 120);

        shortenedText += text.length > shortenedText.length ? '...' : '';

        return '<' + tagName + (attrs ? ' ' + attrs : '') + '>' + shortenedText;
    }

    function stringifyAttrs(node) {
        return $.makeArray(node.attributes)
            .map(function(attr) {
                return attr.name + '="' + attr.value + '"';
            })
            .join(' ');
    }

    function sourceCallback() {
        var args = arguments;

        return function() { return stringifyNode.apply(null, args) };
    }

    /**
     * Attribute event.
     *
     * @event hermione-dom-walker#attribute
     *
     * @mixes hermione-dom-walker#event
     * @property {Attribute} attr - текущий атрибут
     */

    /**
     * Text event.
     *
     * @event hermione-dom-walker#text
     *
     * @mixes hermione-dom-walker#event
     * @property {String} text - содержимое текстовой ноды
     */

    /**
     * Comment event.
     *
     * @event hermione-dom-walker#comment
     *
     * @mixes hermione-dom-walker#event
     * @property {String} comment - содержимое комментария
     */

    /**
     * Node event.
     *
     * @event hermione-dom-walker#node
     *
     * @mixes hermione-dom-walker#event
     * @property {String} tagName - имя тэга в нижнем регистре
     */

    /**
     * Dom walker event.
     *
     * @event hermione-dom-walker#event
     *
     * @type {Object}
     * @property {Node} node - текущий DOM элемент (для текстовых нод и комментариев - ближайший родитель)
     * @property {Function} source - функция, возвращающая информацию о текущем DOM элементе
     */
})(BEM, $);
