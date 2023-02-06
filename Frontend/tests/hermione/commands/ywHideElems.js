/**
 * ywRemoveElems('.some-classes')
 * ywRemoveElems(document.querySelectorAll('.some-classes'))
 *
 * @param {String | Node | Array<String> | NodeListOf<any>} selectors
 * @param {Object}  [opts]
 * @param {Boolean} [opts.remove=false] Удалить dom-ноду (true) или сделать ее невидимой (false)
 * @returns {Promise<void>}
 */
module.exports = async function ywHideElems(selectors, opts = {}) {
    if (!Array.isArray(selectors)) {
        selectors = [selectors];
    }

    selectors = selectors.filter(Boolean);

    selectors.length && await this.executeAsync(function(selectors, opts, done) {
        const remover = opts.remove ?
            node => node && node.parentNode.removeChild(node) :
            node => node && (node.style.display = 'none');

        function removeNodes(nodes) {
            if (!nodes) {
                return;
            }

            nodes.forEach(remover);
        }

        selectors.forEach(selector => {
            typeof selector === 'string' ? removeNodes(document.querySelectorAll(selector)) : removeNodes(selector);
        });

        requestAnimationFrame(done);
    }, selectors, opts);
};
