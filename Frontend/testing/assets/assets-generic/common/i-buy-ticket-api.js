BEM.decl('i-buy-ticket-api', {}, {
    _load: $.noop,
    scan: $.noop,

    /**
     * Найти на странице все теги <y:ticket> и заменить их на соответствующий им шаблон
     *
     * @param {String[]} disabledSelectors - селекторы кнопок, которые не нужно активировать
     * @param {Number} price - цена билетов
     */
    manualScan: function(disabledSelectors, price) {
        disabledSelectors = disabledSelectors || [];

        var yTickets = $('y\\:ticket'),
            yTicketsDisabled = disabledSelectors.reduce(function(result, selector) {
                return result.concat($(selector).toArray());
            }, []);

        yTickets.each(function() {
            var yTicket = $(this),
                isDisabled = yTicketsDisabled.some(function(item) {
                    return item === yTicket.get(0);
                });

            if (isDisabled) return;

            var template = $('#' + yTicket.data('template')),
                link = $('<a />')
                    .attr('href', '#')
                    .attr('style', 'text-decoration: none;')
                    .addClass('js-yaticket-button')
                    .click(function(e) {
                        e.preventDefault();
                    })
                    .html(
                        template.html()
                            .replace(
                                '<y:ticket-content></y:ticket-content>',
                                yTicket.data('content')
                            )
                            .replace(
                                '${call(price)}',
                                price ? (price + ' руб') : ''
                            )
                    );

            yTicket.replaceWith(link);
        });
    }
});
