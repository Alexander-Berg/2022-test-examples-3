/**
 * Хелпер, отключающий анимацию, записанную в style внутри svg.
 *
 * @param {String} selector
 */
module.exports = function(selector) {
    const imageEl = document.querySelector(selector);

    if (!imageEl) return;

    const CLOSE_STYLE_TAG = '%3c/style%3e'; // </style>
    const DISABLE_ANIMATION_RULE = '*%7banimation-duration:0s!important%7d'; // *{animation-duration:0s!important}
    const before = window.getComputedStyle(imageEl).backgroundImage;
    const position = before.toLowerCase().indexOf(CLOSE_STYLE_TAG);
    const after = before.slice(0, position) + DISABLE_ANIMATION_RULE + before.slice(position);

    imageEl.style.backgroundImage = after;
};
