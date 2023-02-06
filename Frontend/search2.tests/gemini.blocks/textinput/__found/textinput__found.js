/**
 * A COPY FROM input__found.js
 */
BEM.DOM.decl('textinput', {
    onSetMod: {
        js: {
            inited: function() {
                this.__base.apply(this, arguments);

                if(this.elem('found').length) {
                    var control = this.elem('control');

                    $('<span/>', {'class': this.buildSelector('query-holder').slice(1)}) // eslint-disable-line
                        .css({
                            'font-family': control.css('font-family'),
                            'font-size': control.css('font-size')
                        })
                        .attr('aria-hidden', 'true')
                        .text(this.getText())
                        .appendTo(this.domElem);

                    this._controlQueryLeftOffset =
                        parseInt(control.css('padding-left'), 10) +
                                 (parseInt(control.css('border-left-width'), 10) || 0);
                    this.on('change', this._toggleVisibility)
                        .bindToWin('resize', $.throttle(this._onWindowResize, 100, this))
                        .setPosition();
                }
            }
        }
    },

    _toggleVisibility: function() {
        // Показываем фразу "— 100500 ответов" если выполняются условия (с учетом input_focused_yes в css):
        var isVisible =
            (this.getText() === this.elem('query-holder').text()) && // Текущий запрос совпадает с запросом выдачи
            (this.textWidth < this.inputWidth) &&                // Проверяем, что для фразы хватает места
            Boolean(this.elem('found').text());                  // Проверяем есть ли фраза (для пустой выдачи ее нет)

        this.toggleMod(this.elem('found'), 'visibility', 'visible', '', isVisible);
    },

    setPosition: function() {
        var control = this.elem('control'),
            found = this.elem('found'),
            queryWidth = // Расстояние от левого края input до левого края __found складывается из:
                control.offset().left - this.domElem.offset().left + // 1) cмещения __control внутри input,
                this._controlQueryLeftOffset +                       // 2) смещения поискового запроса внутри __control,
                this.elem('query-holder').width();                   // 3) и ширины запроса.

        this.inputWidth = control.width();
        this.textWidth = found.width() + queryWidth;

        found.css({left: queryWidth});
        this._toggleVisibility();
    },

    _onWindowResize: function() {
        this.inputWidth = this.elem('control').width();

        this._toggleVisibility();
    },

    setFound: function(found, query) {
        this.elem('found').text(found);
        this.elem('query-holder').text(query);
        this.setPosition();
    }
});
