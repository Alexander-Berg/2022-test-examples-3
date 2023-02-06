/**
 * @returns {any}
 */
module.exports = function yaLoseFocus() {
  return this.execute(() => {
    const focused = document.activeElement;

    if (focused) {
      focused.blur();
    }
  });
};
