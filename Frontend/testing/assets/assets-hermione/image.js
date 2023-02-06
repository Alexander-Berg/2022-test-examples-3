if (window.location.search.indexOf('without_image_stub=1') === -1) {
    modules.define('animation', ['i-bem-dom'], function(provide, bemDom) {
        provide(bemDom.declBlock('animation', {}, { lazyInit: false }));
    });
}
