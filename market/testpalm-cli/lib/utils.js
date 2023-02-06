"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.mapKeys = exports.isString = exports.isPlainObject = exports.isIterable = void 0;

var _ramda = require("ramda");

const isIterable = obj => obj != null && !Object.isSealed(obj) && typeof obj[Symbol.iterator] === 'function';

exports.isIterable = isIterable;

const isPlainObject = obj => typeof obj === 'object' && !isIterable(obj);

exports.isPlainObject = isPlainObject;

const isString = str => typeof str === 'string';

exports.isString = isString;
const mapKeys = (0, _ramda.curry)(function mapKeys(fn, obj) {
  return (0, _ramda.reduce)(function (acc, key) {
    // @ts-ignore
    acc[fn(key)] = obj[key];
    return acc;
  }, {}, (0, _ramda.keys)(obj));
});
exports.mapKeys = mapKeys;