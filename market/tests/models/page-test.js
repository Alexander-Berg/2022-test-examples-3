'use strict';

const _ = require('lodash');
const { expect } = require('chai');

const PageModel = require('../../server/model/page');

describe('Page model', () => {
    const bunker = require('../mock/bunker');
    const pagesRelations = _.chain(bunker).get('settings.ru.relations')
        .indexBy('slug')
        .value();
    const page = new PageModel({
        bunker, pagesRelations
    }, {
        section: 'edu',
        page: 'webinars'
    });

    beforeEach(() => {
        page.sectionNode.webinars.redirect = null;
        page.sectionNode.webinars.enabled = true;
        page.sectionInfo.isProduct = false;
    });

    it('should return correct `section`', () => {
        expect(page.section).to.be.equal('edu');
    });

    it('should return correct `page`', () => {
        expect(page.page).to.be.equal('webinars');
    });

    it('should return correct `bunkerRoot`', () => {
        const nodes = [
            'actions', 'contact', 'edu-pages', 'materials', 'media-cases',
            'media-courses', 'media-stories', 'order-pages', 'partners', 'prices-pages',
            'products-classified', 'products-context', 'products-geo', 'products-mobile',
            'products', 'requirements', 'simple-pages', 'solutions', 'subscription-pages',
            'targetings'
        ];

        expect(page.bunkerRoot).to.contain.all.keys(nodes);
    });

    it('should return correct `sectionInfo`', () => {
        expect(page.sectionInfo).to.be.deep.equal({
            slug: 'edu',
            node: 'edu-pages',
            isProduct: false
        });
    });

    it('should return correct `sectionNode`', () => {
        const nodes = ['_main', 'adfox', 'direct', 'market'];

        expect(page.sectionNode).to.contain.all.keys(nodes);
    });

    it('should return correct `menuRoot`', () => {
        expect(page.menuRoot).to.be.equal('/adv/edu');
    });

    it('should return page data', done => {
        page
            .getData()
            .then(data => {
                expect(data.enabled).to.be.true;
                expect(data).to.contain.all.keys(['media', 'product', 'sections']);

                done();
            })
            .catch(err => {
                done(err);
            });
    });

    it('should return 404 when `isProduct`', done => {
        page.sectionInfo.isProduct = true;

        page
            .getData()
            .catch(err => {
                expect(err).to.deep.equal({
                    internalCode: '404_PNF',
                    message: 'Page /edu/webinars was not found'
                });

                done();
            });
    });

    it('should return 404 when page disabled', done => {
        page.sectionNode.webinars.enabled = false;

        page
            .getData()
            .catch(err => {
                expect(err).to.deep.equal({
                    internalCode: '404_PNF',
                    message: 'Page /edu/webinars was not found'
                });

                done();
            });
    });

    it('should redirect when page disabled and has `redirect` field', done => {
        page.sectionNode.webinars.enabled = false;
        page.sectionNode.webinars.redirect = 'yandex.ru/adv';

        page
            .getData()
            .catch(err => {
                expect(err).to.deep.equal({
                    internalCode: '301_PMP',
                    message: 'Redirect from /edu/webinars',
                    location: 'yandex.ru/adv'
                });

                done();
            });
    });
});
