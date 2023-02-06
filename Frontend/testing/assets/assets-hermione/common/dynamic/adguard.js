(function(document) {
    'use strict';

    var e = document.getElementsByClassName('t-construct-adapter__adv');
    for (var i = 0; i < e.length; ++i) {
        e[i].style.display = 'none';
    }

    var popup = document.getElementsByClassName('distr-popup')[0],
        distro = document.getElementsByClassName('distro')[0];

    if (popup) {
        popup.style.display = 'none';
    }

    if (distro) {
        distro.style.display = 'none';
    }
})(document);
