import {makeCase, makeSuite} from 'ginny';

function makeState(ctx, {
    dataQuestionId,
    slug = 'any',
    canDelete = false,
    text = 'lol',
}) {
    const question = {
        id: dataQuestionId,
        canDelete,
        user: {
            uid: '666',
            entity: 'user',
        },
        author: {
            id: '666',
            entity: 'user',
        },
        slug,
        text,
        category: {
            id: 91491,
            entity: 'category',
        },
        product: {
            id: 14206682,
            entity: 'product',
        },
    };

    const schema = {
        users: [{
            id: '666',
            public_id: 'lolpop112233',
            uid: {
                value: '666',
            },
            login: 'lol',
            display_name: {
                name: 'lop pop',
                public_name: 'Lol P.',
            },
        }],
        modelQuestions: [question],
    };

    return ctx.browser
        .setState('schema', schema)
        .then(() => ctx.browser.refresh());
}

/**
 * @param {PageObject.questionSnippet} questionSnippet
 */
export default makeSuite('Блок примешанного продуктового вопроса.', {
    environment: 'kadavr',
    params: {
        expectedProductLink: 'Ожидаемая ссылка на карточку модели',
        slug: 'Представление вопроса в url',
        dataQuestionId: 'Id текущего вопроса',
    },
    story: {
        'Ссылка на товар': {
            'по умолчанию': {
                'ведёт на карточку модели': makeCase({
                    id: 'm-touch-3030',
                    issue: 'MARKETFRONT-5119',
                    async test() {
                        const {dataQuestionId, slug} = this.params;
                        await makeState(this, {dataQuestionId, slug});

                        const isProductLinkVisible = await this.questionSnippet.isProductLinkVisible();
                        await this.expect(isProductLinkVisible).to.be.equal(true, 'Ссылка на товар отображается');

                        const currentLink = await this.questionSnippet.productLinkHref;
                        await this.expect(currentLink)
                            .to.be.link({pathname: this.params.expectedProductLink}, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        },
    },
});
