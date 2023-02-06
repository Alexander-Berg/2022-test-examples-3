'use strict';

const InitExtensionRequestModel = require('./../../../src/models/InitExtensionRequestModel');

describe('models / InitExtensionRequestModel', () => {
    describe('constructor', () => {
        it('should create an instance of InitExtensionRequestModel if ip and transactionId are not defined',
            () => {
                const requestModel = new InitExtensionRequestModel();

                expect(requestModel).toBeInstanceOf(InitExtensionRequestModel);
            });

        it('should create an instance of InitExtensionRequestModel if ip and transactionId are defined',
            () => {
                const ip = Math.random().toString();
                const transactionId = Math.random().toString();
                const requestModel = new InitExtensionRequestModel({ ip, transactionId });

                expect(requestModel).toBeInstanceOf(InitExtensionRequestModel);
            });

        it('should throw a TypeError if ip is non-string', () => {
            const fn = () => {
                const ip = Math.random();

                new InitExtensionRequestModel({ ip });
            };

            expect(fn).toThrow(TypeError);
        });

        it('should throw a TypeError if transactionId is non-string', () => {
            const fn = () => {
                const transactionId = Math.random();

                new InitExtensionRequestModel({ transactionId });
            };

            expect(fn).toThrow(TypeError);
        });
    });

    describe('#isFirstRequest', () => {
        describe('get', () => {
            it('should return false by default', () => {
                const requestModel = new InitExtensionRequestModel();

                expect(requestModel.isFirstRequest).toBeFalsy();
            });
        });

        describe('set', () => {
            it('should not throw an error if isFirstRequest is not defined', () => {
                const fn = () => {
                    const requestModel = new InitExtensionRequestModel();

                    requestModel.isFirstRequest = undefined;
                };

                expect(fn).not.toThrow();
            });

            it('should not throw an error if isFirstRequest is a boolean', () => {
                const fn = () => {
                    const requestModel = new InitExtensionRequestModel();

                    requestModel.isFirstRequest = true;
                };

                expect(fn).not.toThrow();
            });

            it('should throw a TypeError if isFirstRequest is non-boolean', () => {
                const fn = () => {
                    const requestModel = new InitExtensionRequestModel();

                    requestModel.isFirstRequest = Math.random();
                };

                expect(fn).toThrow(TypeError);
            });
        });
    });
});
