'use strict';

const STUB_CSRF_TOKEN = 'csrf:abcdefghijklmnopqrstuvwxyz';

/**
 * @function
 * @returns {STUB_CSRF_TOKEN}
 */
const generateToken = () => {
  return STUB_CSRF_TOKEN;
};

/**
 * @function
 * @returns {true}
 */
const isTokenValid = () => {
  return true;
};

/**
 * @class
 * @returns {{ generateToken: Function, isTokenValid: Function }}
 */
function Csrf() {
  return { generateToken, isTokenValid };
}

module.exports = { Csrf };
