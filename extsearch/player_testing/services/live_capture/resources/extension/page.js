function requestClick(x, y) {
    var absX = window.screenX + x;
    var absY = window.screenY + y;
    console.log('requesting click to (' + absX + ',' + absY + ')');
    var xhr = new XMLHttpRequest();
    xhr.open("GET", "http://localhost:8001/js/click?x=" + absX + "&y=" + absY, true);
    xhr.send();
}

function startPlayerDefault() {
    document.querySelector('body').onclick = null;
    setTimeout(function() {
        console.log('trying to start player');
        requestClick(window.innerWidth / 2, window.innerHeight / 2);
    }, 1000);
}

function getPlayerElement(doStart) {
    let host = window.location.hostname;
    var elem = null;
    var needStart = false;
    if (host == "www.youtube.com") {
        elem = document.querySelector('button.ytp-fullscreen-button.ytp-button');
        if (elem !== null && doStart == true) {
            elem.click();
        }
    } else if (host == "www.1tv.ru") {
        elem = document.getElementById('embedlive_remote');
    } else if (host == "live.russia.tv") {
        elem = document.querySelector('div.player-wrap');
    } else if (host == "glaz.tv") {
        elem = document.getElementById('div.video-player-container');
    } else if (host == "ctc.ru") {
        elem = document.querySelector('div.playerLive');
    } else if (host == 'www.5-tv.ru') {
        elem = document.querySelector('iframe#AboutVideoIframe');
    } else if (host == 'www.tvc.ru') {
        elem = document.querySelector('div.channel__inner');
        needStart = true;
    } else if (host == 'tv3.ru') {
        elem = document.querySelector('div.app-video-player--wrap :first-child');
        needStart = true;
    } else if (host == 'ren.tv') {
        elem = document.querySelector('div.iframe-video');
    } else if (host == 'domashniy.ru') {
        elem = document.querySelector('div.player-item__content.player-item__content--live :first-child');
    } else if (host == "telik.top") {
        elem = document.querySelector('div#player');
        needStart = true;
    } else if (host == "matchtv.ru") {
        elem = document.querySelector('iframe.video-player__iframe ');
        needStart = true;
    } else if (host == "ntv.ru") {
        //XXX not working
        elem = document.querySelector('div.playerBody');
        needStart = true;
    } else if (host == "friday.ru") {
        //XXX not working
        elem = document.querySelector('iframe');
        needStart = true;
    } else if (host == "tnt-online.ru") {
        elem = document.querySelector('iframe.live__frame');
        //needStart = true;
    } else if (host == "rutube.ru") {
        elem = document.querySelector('div.live-page__player');
        needStart = true;
    } else {
        console.log('getPlayerElement: unknown host ' + host);
        return elem;
    }
    if (doStart == true && needStart == true && elem !== null) {
        startPlayerDefault();
    }
    console.log('getPlayerElement: ' + host + ' ' + elem);
    return elem;
}

function fullscreenWatchdog() {
    if (!document.fullscreenElement) {
        console.log('fullscreen status lost');
        getPlayerElement().requestFullscreen();
    }
    setTimeout(fullscreenWatchdog, 1000);
}

function requestFullscreenClick() {
    if (getPlayerElement(false) === null) {
        window.setTimeout(requestFullscreenClick, 100);
        return;
    }
    let el = document.createElement('div');
    el.id='ydx_fs_button';
    el.style.position='fixed';
    el.style.top='0px';
    el.style.left='0px';
    el.style.background='green';
    el.style.width='50px';
    el.style.height='50px';
    el.style.zIndex=9999;
    el.onclick = function() {
        console.log('got fullscreen click');
        this.style.display='none';
        let elem = getPlayerElement(true);
        if (elem === null) {
            console.log('player container not found');
        } else if (window.location.hostname != "www.youtube.com") {
            console.log('player container found, requesting fullscreen');
            elem.requestFullscreen().catch(err => {
                console.log('error entering fullscreen: ' + err.message + ' ' + err.name);
            }).then((val) => {
                fullscreenWatchdog();
            });
        }
    }
    document.querySelector('body').appendChild(el);
    let rect = el.getBoundingClientRect();
    requestClick(rect.left + rect.width / 2, rect.top + rect.height / 2);
    console.log('fullscreen click requested');
    setTimeout(function() {
        var xhr = new XMLHttpRequest();
        xhr.open("GET", "http://localhost:8001/js/stream", true);
        xhr.send()
    }, 500);
}

console.log('1tv_live script hello');

window.setInterval(function() {
    var xhr = new XMLHttpRequest();
    xhr.open("GET", "http://localhost:8001/js/ping", true);
    xhr.onreadystatechange = function() {
        if(xhr.readyState === XMLHttpRequest.DONE && xhr.status === 200) {
            if (xhr.responseText !== 'OK') {
                console.log('have to close window', window.location.href)
                window.close();
            }
        }
    };
    xhr.send();
}, 5000);

requestFullscreenClick();

console.log('1tv_live script done');
