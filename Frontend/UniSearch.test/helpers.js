const helpers = require('../../../../hermione/utils/baobab');

async function checkNodeExists({ path, attrs }) {
    const app = await this.browser.getMeta('app');
    const isSearchApp = app === 'searchapp-phone';

    if (process.env.SPAS_AJAX && !isSearchApp) {
        path = path.replace('$page', '$subresult');
    }

    return await this.browser.yaAssertCounters(({ trees }) => {
        const check = trees.some(tree => {
            return helpers.query(
                path,
                tree.tree,
                attrs,
            ).length > 0;
        });

        if (!check) {
            const err = new Error(`В Баобаб не найден узел по пути ${path}, с аттрибутами ${JSON.stringify(attrs || {})}`);
            trees.forEach(item => helpers.getPaths(item.tree));
            err.details = { title: 'triggered counters', data: trees };
            throw err;
        }
    });
}

module.exports = {
    checkNodeExists,
};
