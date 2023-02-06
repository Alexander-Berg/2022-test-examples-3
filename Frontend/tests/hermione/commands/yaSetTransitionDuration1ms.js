/**
 *
 * Команда для переопределения длительности анимаций, чтобы скрины не моргали.
 */
module.exports = function() {
    return this
        .execute(() => {
            // 1ms а не 0, т.к. иначе не будут срабатывать transitionend
            const css = '*, :before, :after {' +
                'transition-delay: 1ms !important;' +
                'transition-duration: 1ms !important;' +
                'animation-duration: 1ms !important;' +
                'animation-delay: 1ms !important;' +
                '}';

            let head = document.head;
            let style = document.createElement('style');

            style.type = 'text/css';
            style.appendChild(document.createTextNode(css));

            head.appendChild(style);
        });
};
