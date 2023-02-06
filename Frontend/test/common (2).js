'use strict';

var express = require('express'),
    uatraits = require('express-uatraits-mock'),
    cookieParser = require('cookie-parser'),
    expressYandexUid = require('..');

function getApp(uatraitsOptions) {
    return express()
        .use(cookieParser())
        .use(uatraits(uatraitsOptions))
        .use(expressYandexUid())
        .get('/', function(req, res) { res.status(200).end() });
}

function shouldNotRedirectOrSetCookie(done) {
    return function(err, res) {
        if (err) { return done(err) }
        if ('set-cookie' in res.headers) { return done(new Error('Cookies should not be updated')) }
        if ('location' in res.headers) { return done(new Error('Should not have redirect')) }
        done();
    };
}

function shouldNotRedirect(done) {
    return function(err, res) {
        if (err) { return done(err) }
        if ('location' in res.headers) { return done(new Error('Should not have redirect')) }
        done();
    };
}

exports.getApp = getApp;
exports.shouldNotRedirect = shouldNotRedirect;
exports.shouldNotRedirectOrSetCookie = shouldNotRedirectOrSetCookie;
