BEM.DOM.decl({ name: 'series-navigator', modName: 'showcase', modVal: 'yes' }, {
    _subscribeShowcaseMore: function() {
        if (!this._hasMore()) return;

        var scrollLeft = this._getShowcase()._scroller.getScrollLeft();
        var scrollTop = this._getShowcase()._scroller.getScrollTop();

        // Блок навигатора инициализируется по скроллу шоукейса внутри него. Может возникнуть ситуация, когда серии
        // проскроллены до конца после первого события scroll и второго события не наступит, соответственно дозагрузка
        // не стартанет, а пользователь подумает, что доскроллил до конца и серий больше нет.
        if (scrollLeft > 0 || scrollTop > 0) {
            // проверяем в момент инициализации был ли скролл
            this._loadMoreOnCondition();
        }

        // сюда попадеам, если блок был проинициалирован не скроллом
        this._getShowcase().on('scroll', this._loadMoreOnCondition, this);
    }
});
