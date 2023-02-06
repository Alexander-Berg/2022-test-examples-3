cat >> /etc/apt/sources.list << EOF
deb http://mirror.yandex.ru/ubuntu/ bionic main restricted universe multiverse
deb http://mirror.yandex.ru/ubuntu/ bionic-updates main restricted universe multiverse
EOF

apt-get update && \
apt-get -y install \
    python2.7 python-dev python-pip python-setuptools \
    imagemagick xvfb x11vnc xdotool openbox chromium-browser chromium-chromedriver firefox firefox-geckodriver \
    ffmpeg pulseaudio vlc yandex-porto

adduser --home /work --disabled-password snail
