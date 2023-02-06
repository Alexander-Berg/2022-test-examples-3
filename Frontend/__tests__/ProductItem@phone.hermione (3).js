function ensureRectsAreEqual(productItemSelector, productItemLinkSelector, browser) {
    return browser
        .execute(({ productItemSelector, productItemLinkSelector }) => {
            const outer = document.querySelector(productItemSelector);
            const inner = outer.querySelector(productItemLinkSelector);
            const outerStyle = window.getComputedStyle(outer);
            const innerStyle = window.getComputedStyle(inner);

            const innerWidth = innerStyle.getPropertyValue('width');
            const innerHeight = innerStyle.getPropertyValue('height');
            const outerWidth = outerStyle.getPropertyValue('width');
            const outerHeight = outerStyle.getPropertyValue('height');

            return { innerWidth, innerHeight, outerWidth, outerHeight };
        }, { productItemSelector, productItemLinkSelector })
        .then(({ value: { outerWidth, outerHeight, innerWidth, innerHeight } }) => {
            assert.equal(outerWidth, innerWidth, 'Карточка товара кликается по всей области');
            assert.equal(outerHeight, innerHeight, 'Карточка товара кликается по всей области');
        });
}

const layouts = {
    default: { stub: 'default.json' },
    list: { stub: 'type-list.json' },
    'big-list': { stub: 'type-big-list.json' },
};

specs({
    feature: 'product-item',
}, () => {
    describe('Внешний вид', () => {
        Object.entries(layouts).forEach(([layoutName, params]) => {
            hermione.only.notIn('safari13');
            it(`Карточка list-type ${layoutName}`, function() {
                return this.browser
                    .url(`?stub=productitem/${params.stub}&exp_flags=turboforms_endpoint=/`)
                    .yaWaitForVisible(PO.page())
                    .yaIndexify(PO.blocks.productItem())
                    .assertView('plain', PO.page());
            });
        });
    });

    describe('Функциональность', () => {
        const productItemSelector = PO.blocks.productItem();
        const productItemLinkSelector = PO.blocks.productItem.link();

        Object.entries(layouts).forEach(([layoutName, params]) => {
            hermione.only.in(['iphone'], 'Ускоряем браузеронезависимые тесты');
            hermione.only.notIn('safari13');
            it(`Карточка list-type ${layoutName}`, function() {
                return this.browser
                    .url(`?stub=productitem/${params.stub}&exp_flags=turboforms_endpoint=/`)
                    .yaWaitForVisible(PO.blocks.page())
                    .then(() => ensureRectsAreEqual(productItemSelector, productItemLinkSelector, this.browser));
            });
        });
    });

    hermione.only.notIn('safari13');
    it('С прокрашенным текстом в названии', function() {
        return this.browser
            .url('?stub=productitem/with-format-in-desc.json')
            .yaWaitForVisible(PO.blocks.productItem())
            .assertView('plain', PO.blocks.productItem());
    });

    hermione.only.in(['iphone'], 'Ускоряем браузеронезависимые тесты');
    hermione.only.notIn('safari13');
    it('С разным количеством опций', function() {
        return this.browser
            .url('?stub=productitem/with-different-options-count.json')
            .yaWaitForVisible(PO.page())
            .assertView('plain', PO.page());
    });

    hermione.only.in(['iphone'], 'Ускоряем браузеронезависимые тесты');
    hermione.only.notIn('safari13');
    it('С "В корзину" для внешней корзины', function() {
        return this.browser
            .url('?stub=productitem/default.json&exp_flags=analytics-disabled=0')
            .yaIndexify(PO.blocks.productItem())
            .yaCheckLink({
                selector: PO.blocks.firstProductItem.footer.button(),
                message: 'Ссылка "В корзину" не ведет на внешнюю корзину',
                target: '_blank',
                url: {
                    href: 'https://www.kupivip.ru/basketAdd?skuNumber=W18112906346&russianSize=42',
                },
            })
            .yaCheckMetrikaGoals({
                '11111111': ['add-to-cart'],
            });
    });
});
