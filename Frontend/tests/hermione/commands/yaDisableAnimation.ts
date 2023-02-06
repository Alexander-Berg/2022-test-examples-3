/**
 * Отключает анимацию на всех блоках
 */
module.exports = async function yaDisableAnimation() {
    await this.execute(function () {
        const style = document.createElement('style');

        style.type = 'text/css';
        style.innerHTML = [
            '* {',
            '-webkit-animation-play-state: paused !important;',
            '-ms-animation-play-state: paused !important;',
            'animation-play-state: paused !important;',
            '-webkit-animation: none !important;',
            '-ms-animation: none !important;',
            'animation: none !important;',
            'transition: none !important',
            '}',
        ].join('');

        document.head.appendChild(style);
    });
};
