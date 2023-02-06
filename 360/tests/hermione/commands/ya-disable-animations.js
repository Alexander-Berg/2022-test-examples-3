/**
 * @see https://github.yandex-team.ru/search-interfaces/frontend/blob/master/services/abc/tools/hermione/custom-commands/disable-animations.js
 */

/**
 * @param  {...string} args - Selectors.
 * @returns {any}
 */
module.exports = function yaDisableAnimations(...args) {
  return this.execute(selectors => {
    const selector = [].concat(selectors).join(', ');
    const style = document.createElement('style');

    style.textContent = `${selector}
      {
        animation: none !important;
        transition-property: none !important;
        -webkit-transition-property: none !important;
      }`;

    document.head.appendChild(style);
  }, ...args);
};
