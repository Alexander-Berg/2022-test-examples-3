function tryUnmute() {
    var el = document.querySelector('div.eump-reset.eump-unmute');
    if (el == null) {
        el = document.querySelector('div.pl-vid__mute-alert');
        if (el == null) {
            el = document.querySelector('div.pony-svg-container.pony-icon-volume-mute');
        }
    }
    if (el == null) {
        window.setTimeout(tryUnmute, 3000);
        return;
    }
    var bbox = el.getBoundingClientRect();
    console.log('unmute', bbox.x, bbox.y, bbox.width, bbox.height);
    if (bbox.x !== 0 && bbox.y !== 0) {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', 'http://localhost:8001/js/click?x=' + (bbox.x + 1) + '&y=' + (bbox.y + 1) , true);
        xhr.send();
    } else {
        window.setTimeout(tryUnmute, 1000);
    }
}
console.log('unmute script hello from ' + document.location.href);
tryUnmute();
