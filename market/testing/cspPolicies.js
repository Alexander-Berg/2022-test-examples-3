'use strict';

const {YA_STATIC_BETA} = require('../csp/constants');
const {mergePolicies} = require('../csp/helpers');
const {baseExtensionPolicies, baseWebEmbedPolicies, baseWebLocalPolicies} = require('../csp/basePolicies');

const testingPolicies = {
    'font-src': [YA_STATIC_BETA],

    'img-src': [YA_STATIC_BETA],

    'script-src': [YA_STATIC_BETA],

    'style-src': [YA_STATIC_BETA],
};

const cspExtensionPolicies = mergePolicies(baseExtensionPolicies, testingPolicies);

const cspWebLocalPolicies = mergePolicies(baseWebLocalPolicies, testingPolicies);

const cspWebEmbedPolicies = mergePolicies(baseWebEmbedPolicies, testingPolicies);

module.exports = {
    cspExtensionPolicies,
    cspWebEmbedPolicies,
    cspWebLocalPolicies,
};
