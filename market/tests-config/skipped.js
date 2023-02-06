const {forPlatform} = require('./textFormat');

// скип-пак переехал в текстовые файлы skipped.*.txt
module.exports = {
    partner_desktop: forPlatform('desktop'),
    partner_touch: forPlatform('touch'),
    adv: forPlatform('adv'),
};

// MARKETPARTNER-37055 перенос ADV
if (!global.it) {
    const {exports} = module;
    const {env} = process;

    if (env.HERMIONE_CASES_SELECTION !== 'skipped') {
        exports.partner_desktop = [...exports.partner_desktop, ...exports.adv];
    } else if (env.PLATFORM_TYPE === 'adv') {
        // HACK skip^2: вырезаем из adv заскипанное в desktop
        const releaseIssues = new Set(String(env.AT_RELEASE_ISSUES || '').split(/\s+/));
        const desktopNames = exports.partner_desktop.reduce((a, skip) => {
            if (!releaseIssues.has(skip.issue)) {
                a.push(...skip.cases.map(c => c.fullName));
            }
            return a;
        }, []);
        const skipSet = new Set(desktopNames);
        const verbose = env.debug;
        for (const skip of exports.adv) {
            if (verbose) {
                for (const {id, fullName} of skip.cases) {
                    if (skipSet.has(fullName)) {
                        console.warn('ADV-SKIP', id, fullName);
                    }
                }
            }
            skip.cases = skip.cases.filter(c => !skipSet.has(c.fullName));
        }
    }
}
