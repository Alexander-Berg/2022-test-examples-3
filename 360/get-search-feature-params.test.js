'use strict';

const getSearchFeatureParams = require('./get-search-feature-params.js');

test('should return empty object if params are not passed', function() {
    expect(getSearchFeatureParams()).toEqual({});
});

test('should return empty object if _features does not exist', function() {
    expect(getSearchFeatureParams({})).toEqual({});
});

test('should return empty object if _features does not have web_search_extra_params', function() {
    expect(getSearchFeatureParams({ _features: {} })).toEqual({});
});

test('should return object with fields from _features.web_search_extra_params', function() {
    expect(
        getSearchFeatureParams({
            _features: {
                web_search_extra_params: {
                    test1: true,
                    test2: '123',
                    test3: 123,
                    test4: {
                        data: true
                    }
                }
            }
        })
    ).toEqual({
        test1: true,
        test2: '123',
        test3: 123,
        test4: {
            data: true
        }
    });
});
