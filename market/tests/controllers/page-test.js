'use strict';

const _ = require('lodash');
const { expect } = require('chai');
const middlewareMock = require('../helper/middleware-mock');

const Page = require('../../server/controllers/page');

describe('Page controller', () => {
    let page = new Page({
        params: {
            section: 'edu',
            page: 'webinars'
        }
    });
    const req = { tld: 'ru' };
    const res = { renderWithLocals: middlewareMock.renderWithLocals() };

    before(() => {
        req.pagesRelations = {
            a: {
                slug: 'a',
                node: 'a-pages',
                isProduct: false,
                menu: 'a-menu'
            }
        };
        req.bunker = {
            sources: {
                ru: {
                    'a-pages': {
                        _main: {
                            cta: 'Section CTA',
                            phone: 'Section phone',
                            sectionTitle: 'Section title'
                        },
                        b: {
                            enabled: true,
                            title: 'Page title',
                            image: '',
                            text: 'Page text',
                            teaser: '',
                            redirect: '',
                            cta: {
                                text: '',
                                url: ''
                            },
                            tags: []
                        }
                    }
                }
            },
            menu: {
                'a-menu': {
                    ru: [
                        { name: '', url: '', items: [] }
                    ]
                }
            },
            settings: { ru: { products: { text: '' } } },
            seo: {
                ru: {
                    sections: [
                        {
                            id: 'a',
                            title: 'SEO title',
                            description: '',
                            keywords: 'SEO keywords'
                        }
                    ],
                    defaults: {
                        title: 'SEO default title',
                        description: 'SEO default description',
                        keywords: 'SEO default keywords'
                    }
                }
            }
        };
    });

    it('should return correct `pageName`', () => {
        expect(page.pageName).to.be.equal('products');
    });

    it('should return correct `section`', () => {
        expect(page.section).to.be.equal('edu');
    });

    it('should return correct `page`', () => {
        expect(page.page).to.be.equal('webinars');
    });

    it('should return correct `_main` if `page` not specified', () => {
        const rootPage = new Page({ params: { section: 'edu' } });

        expect(rootPage.page).to.be.equal('_main');
    });

    it('should return correct `seoSection`', () => {
        expect(page.seoSection).to.be.equal('edu');
    });

    it('should return Model as PageModel', () => {
        const instance = new page.Model({}, {});

        expect(instance.constructor.name).to.be.equal('Page');
    });

    it('should process data in `get` method', () => {
        const request = Object.assign(req, {
            params: {
                section: 'a',
                page: 'b'
            }
        });

        page = new Page(request, res);
        const standard = {
            section: 'a',
            isRoot: false,
            productsSettings: '',
            product: {
                title: 'Page title',
                sectionTitle: 'Section title',
                image: '',
                text: 'Page text',
                teaser: '',
                cta: 'Section CTA',
                phone: 'Section phone'
            },
            media: { stories: [], cases: [], courses: [] },
            sections: [],
            meta: { noindex: false },
            regions: {},
            seo: {
                title: 'SEO title',
                description: 'SEO default description',
                keywords: 'SEO keywords'
            },
            og: { title: 'Page title', description: 'Page text' }
        };

        return page.get().then(data => {
            const compact = _(data)
                .pick(key => typeof key !== 'undefined')
                .value();

            expect(compact).to.deep.equal(standard);
        });
    });

    it('should throw 404 if `section` not defined', () => {
        const badPage = new Page({});

        try {
            badPage.get();
        } catch (e) {
            expect(e.status).to.be.equal(404);
            expect(e.message).to.be.equal('Not found');
        }
    });
});
