// IE11 не может вычислить размеры svg, только после вставки в DOM
BEM.DOM.decl('bno-1org', {
    getImageSize: function(url, cb) {
        var img = new Image();

        img.onload = img.onerror = function() {
            document.body.appendChild(this);
            cb({ w: this.offsetWidth, h: this.offsetHeight, url: url });
            document.body.removeChild(this);
        };

        img.src = url;
    }
});
