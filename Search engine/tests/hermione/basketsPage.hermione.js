describe('BasketsPage', async function () {
    const testBasketTabs = ({id, type}) => {
        it(`${type} basket - queries`, async function () {
            await this.browser.url(`/baskets/${id}/queries`);
            await this.browser.waitForFetching();
            await this.browser.pause(1000);
            await this.browser.assertBodyView(
                `BasketsPage-basket-${type}-queries`,
            );
        });

        it(`${type} basket - revisions`, async function () {
            await this.browser.url(`/baskets/${id}/revisions`);
            await this.browser.waitForFetching();
            await this.browser.pause(1000);
            await this.browser.assertBodyView(
                `BasketsPage-basket-${type}-revisions`,
            );

            const basketRevisionName = '[data-test-id=basketRevName]';
            await this.browser.$(basketRevisionName).click();
            await this.browser.waitForFetching();
            await this.browser.pause(1000);
            await this.browser.assertBodyView(
                `BasketsPage-basket-${type}-previous-revision`,
            );
        });

        it(`${type} basket - children`, async function () {
            await this.browser.url(`/baskets/${id}/children`);
            await this.browser.waitForFetching();
            await this.browser.pause(1000);
            await this.browser.assertBodyView(
                `BasketsPage-basket-${type}-children`,
            );
        });

        it(`${type} basket - parents`, async function () {
            await this.browser.url(`/baskets/${id}/parents`);
            await this.browser.waitForFetching();
            await this.browser.pause(1000);
            await this.browser.assertBodyView(
                `BasketsPage-basket-${type}-parents`,
            );
        });
    };

    const testBasketEditForm = ({id, type}) => {
        it(`${type} basket - edit form`, async function () {
            await this.browser.url(`/baskets/${id}/form`);
            await this.browser.pause(1000);

            if (type === 'RAW') {
                await this.browser.$('.Mx-Tab=CSV').click();
                await this.browser.assertBodyView(
                    `BasketsPage-basket-edit-form-${type}-csv`,
                );

                await this.browser.$('.Mx-Tab=JSON').click();
                await this.browser.assertBodyView(
                    `BasketsPage-basket-edit-form-${type}-json`,
                );

                await this.browser.$('.Mx-Tab=Preview').click();
                await this.browser.assertBodyView(
                    `BasketsPage-basket-edit-form-${type}-preview`,
                );
            } else {
                await this.browser.assertBodyView(
                    `BasketsPage-basket-edit-form-${type}`,
                );
            }
        });
    };

    const testBasket = ({id, type}) => {
        testBasketTabs({id, type});
        testBasketEditForm({id, type});
    };

    it('should open list', async function () {
        await this.browser.url('/baskets');

        await this.browser.waitForFetching();
        await this.browser.assertBodyView('BasketsPage-list');
    });

    it('should open list with params', async function () {
        await this.browser.url(
            '/baskets?group=Geo&page=0&text=image&isComposite=true&space=LEARN',
        );

        await this.browser.waitForFetching();
        await this.browser.assertBodyView('BasketsPage-list-with-params');
    });

    it('should open list with author', async function () {
        await this.browser.url('/baskets?author=nata-tischenko');

        await this.browser.waitForFetching();
        await this.browser.assertBodyView('BasketsPage-list-with-author');
    });

    it('should open new', async function () {
        await this.browser.url('/baskets/new');

        await this.browser.$('.ButtonGroup').$('button=Raw').click();
        await this.browser.assertBodyView('BasketsPage-new-raw');
        await this.browser.$('.ButtonGroup').$('button=Merging').click();
        await this.browser.assertBodyView('BasketsPage-new-merging');
        await this.browser.$('.ButtonGroup').$('button=Intersecting').click();
        await this.browser.assertBodyView('BasketsPage-new-intersecting');
        await this.browser.$('.ButtonGroup').$('button=Filtering').click();
        await this.browser.assertBodyView('BasketsPage-new-filtering');

        await this.browser.$('.Tumbler-Button').click();
        await this.browser.assertBodyView('BasketsPage-new-from-json');
    });

    testBasket({id: 298933, type: 'RAW'});
    testBasket({id: 298936, type: 'FILTERED'});
    testBasket({id: 298935, type: 'MERGING'});
});
