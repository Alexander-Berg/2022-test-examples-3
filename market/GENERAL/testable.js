const {nonGenerated, nonGenerateds} = require('./non-generated');
const {addAliases} = require('./transform-alias');

async function pkgs() {
    const repo = require('../models/repo');
    const packages = await repo.packages();

    const scripts = await Promise.all(packages.map(async function (pkg) {
        const {scripts} = await pkg.config();
        return {pkg, scripts};
    }));

    return scripts.filter(function ({scripts}) {
        return scripts && scripts.test;
    }).map(({pkg}) => pkg.name);
}

const testables = Object.create(nonGenerateds);
testables.default = pkgs;
testables.choices = addAliases(pkgs);

const testable = Object.create(nonGenerated);
testable.choices = addAliases(pkgs);

module.exports = {testables, testable};
