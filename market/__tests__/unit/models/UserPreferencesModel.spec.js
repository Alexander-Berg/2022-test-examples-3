'use strict';

const UserPreferencesModel = require('../../../src/models/ClientModel/UserPreferencesModel');

describe('models / UserPreferencesModel', () => {
    const generateCity = () => {
        return { id: Math.random(), name: Math.random().toString() };
    };

    describe('constructor', () => {
        it('should create an instance of UserPreferencesModel if \'city\' is correct', () => {
            const city = generateCity();
            const userPreferencesModel = new UserPreferencesModel({ city });

            expect(userPreferencesModel).toBeInstanceOf(UserPreferencesModel);
        });

        it('shouldn\'t throw an error if \'city\' is not defined', () => {
            const fn = () => new UserPreferencesModel();

            expect(fn).not.toThrow();
        });

        it('should throw a TypeError if \'city\' isn\'t correct', () => {
            const city = null;
            const fn = () => new UserPreferencesModel({ city });

            expect(fn).toThrow(TypeError);
        });
    });

    describe('#city', () => {
        describe('get', () => {
            it('should return correct city', () => {
                const city = generateCity();
                const userPreferencesModel = new UserPreferencesModel({ city });

                expect(userPreferencesModel.city).toEqual(city);
            });
        });

        describe('set', () => {
            it('shouldn\'t throw an error if \'city\' is not defined', () => {
                const fn = () => {
                    new UserPreferencesModel();
                };

                expect(fn).not.toThrow();
            });

            it('shouldn\'t throw an error if \'city\' is correct', () => {
                const fn = () => {
                    const city = generateCity();
                    new UserPreferencesModel({ city });
                };

                expect(fn).not.toThrow();
            });

            it('should throw a TypeError if \'city.id\' is non-number', () => {
                const fn = () => {
                    const city = { id: Math.random().toString(), name: Math.random().toString() };
                    new UserPreferencesModel({ city });
                };

                expect(fn).toThrow(TypeError);
            });

            it('should throw a TypeError if \'city.name\' is non-string', () => {
                const fn = () => {
                    const city = { id: Math.random(), name: Math.random() };
                    new UserPreferencesModel({ city });
                };

                expect(fn).toThrow(TypeError);
            });
        });
    });
});
