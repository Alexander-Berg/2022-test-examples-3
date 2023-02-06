'use strict';

const DesktopUserPreferencesModel = require('../../../src/models/DesktopClientModel/DesktopUserPreferencesModel');

describe('models / DesktopUserPreferencesModel', () => {
    const generateCity = () => {
        return { id: Math.random(), name: Math.random().toString() };
    };

    const generateNeedShowPopupOnBarHover = () => Math.random() > 0.5;

    describe('constructor', () => {
        it('should create an instance of DesktopUserPreferencesModel if \'needShowPopupOnBarHover\' is correct', () => {
            const city = generateCity();
            const needShowPopupOnBarHover = generateNeedShowPopupOnBarHover();
            const userPreferencesModel = new DesktopUserPreferencesModel({ city, needShowPopupOnBarHover });

            expect(userPreferencesModel).toBeInstanceOf(DesktopUserPreferencesModel);
        });

        it('shouldn\'t throw an error if \'city\' is not defined', () => {
            const fn = () => new DesktopUserPreferencesModel();

            expect(fn).not.toThrow();
        });

        it('should throw a TypeError if \'city\' isn\'t correct', () => {
            const city = null;
            const fn = () => new DesktopUserPreferencesModel({ city });

            expect(fn).toThrow(TypeError);
        });

        it('shouldn\'t throw an error if \'needShowPopupOnBarHover\' is not defined', () => {
            const city = generateCity();
            const fn = () => new DesktopUserPreferencesModel({ city });

            expect(fn).not.toThrow();

        });

        it('shouldn\'t throw a TypeError if \'needShowPopupOnBarHover\' is non-boolean', () => {
            const city = generateCity();
            const needShowPopupOnBarHover = Math.random();
            const fn = () => new DesktopUserPreferencesModel({ city, needShowPopupOnBarHover });

            expect(fn).toThrow(TypeError);
        });
    });

    describe('#needShowPopupOnBarHover', () => {
        describe('get', () => {
            it('should return \'true\' by default', () => {
                const userPreferencesModel = new DesktopUserPreferencesModel();

                expect(userPreferencesModel.needShowPopupOnBarHover).toBeTruthy();
            });

            it('should return correct value', () => {
                const needShowPopupOnBarHover = generateNeedShowPopupOnBarHover();
                const userPreferencesModel = new DesktopUserPreferencesModel({ needShowPopupOnBarHover });

                expect(userPreferencesModel.needShowPopupOnBarHover).toBe(needShowPopupOnBarHover);
            });
        });

        describe('set', () => {
            it('shouldn\'t throw an error if \'needShowPopupOnBarHover\' is not defined', () => {
                const fn = () => {
                    new DesktopUserPreferencesModel();
                };

                expect(fn).not.toThrow();
            });

            it('shouldn\'t throw an error if \'needShowPopupOnBarHover\' is boolean', () => {
                const fn = () => {
                    const needShowPopupOnBarHover = generateNeedShowPopupOnBarHover();
                    new DesktopUserPreferencesModel({ needShowPopupOnBarHover });
                };

                expect(fn).not.toThrow();
            });

            it('should throw a TypeError if \'needShowPopupOnBarHover\' is non-boolean', () => {
                const fn = () => {
                    const needShowPopupOnBarHover = Math.random().toString();
                    new DesktopUserPreferencesModel({ needShowPopupOnBarHover });
                };

                expect(fn).toThrow(TypeError);
            });
        });
    });
});
