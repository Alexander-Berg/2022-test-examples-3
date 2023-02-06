const elemId = 'hermione__yaAssertViewportView';

/**
 * Скриншотит исключительно содержимое в пределах текущего вьюпорта.
 * Принимает первый и последний параметры обычного assertView() (кроме селектора).
 */
module.exports = async function yaAssertViewportView(state, opts) {
    await this.execute(function(id) {
        let elem = document.createElement('div');
        elem.id = id;
        elem.setAttribute('style', 'position: fixed; top: 0; right: 0; bottom: 0; left: 0;');
        document.body.appendChild(elem);
    }, elemId);
    await this.assertView(state, `#${elemId}`, opts);
    await this.execute(function(id) {
        const elem = document.getElementById(id);
        elem.parentNode.removeChild(elem);
    }, elemId);
};
