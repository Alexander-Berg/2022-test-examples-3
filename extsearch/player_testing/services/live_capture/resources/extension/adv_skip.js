//videoAdUiSkipButton videoAdUiAction videoAdUiRedesignedSkipButton

function checkAdv() {
    var el = document.querySelector('button.videoAdUiSkipButton');
    if (el == null) {
        el = document.querySelector('button.ytp-ad-skip-button.ytp-button');
    }
    if (el != null) {
        console.log('adv checker fired on', window.location.href);
        el.click();
    }
    window.setTimeout(checkAdv, 5000);
}

function hideYoutubePremium() {
    let el = document.querySelector('ytd-button-renderer#dismiss-button');
    if (el !== null) {
        el.click();
    } else {
        window.setTimeout(hideYoutubePremium, 500);
    }
}

console.log('setting adv checker for', window.location.href);
hideYoutubePremium();
window.setTimeout(checkAdv, 5000);

