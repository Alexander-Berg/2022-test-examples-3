#!/bin/bash

set -e

SELF=$(readlink $0 || echo $0)
BIN=$(dirname $SELF)
OS=$(uname)

HOST=${1:-localhost:80}
TESTS=${2:-report}
HTTP_TIMEOUT=180
export HTTP_TIMEOUT
echo 'Setup prerequisites....'

set -x

if [ "$OS" == "Linux" ]; then
    sudo add-apt-repository 'deb http://dist.yandex.ru/common unstable/all/'
    sudo add-apt-repository 'deb http://dist.yandex.ru/common stable/all/'
    sudo apt-get update
    sudo apt-get -y install python-pip 
    sudo -H pip install --upgrade pip --default-timeout=180
    sudo apt-get -y install python-dev python-setuptools python-requests libxml2-dev libxslt1-dev libz-dev
    sudo apt-get -y install libffi-dev libssl-dev
    sudo apt install -y yandex-yt-python
    sudo apt install -y yandex-yt-python-yson
    sudo apt --auto-remove --yes purge python-openssl
    sudo apt install --reinstall python-openssl -y
    sudo python -m easy_install --upgrade pyOpenSSL
    sudo -H pip install --upgrade cryptography --default-timeout=180
    sudo -H pip install --upgrade pytest --default-timeout=180

    PDIR=`pwd`
    ZDIR="${HOME}/zstd"
    [ -d ${ZDIR} ] && sudo rm -rf ${ZDIR}
    mkdir -p ${ZDIR}
    cd ${ZDIR} && git clone https://github.com/indygreg/python-zstandard.git ${ZDIR} && sudo python setup.py --legacy install
    cd ${PDIR} && [ -d ${ZDIR} ] && sudo rm -rf ${ZDIR}

elif [ "$OS" == "Darwin" ]; then
    if ! which port > /dev/null; then
        echo "ERROR: 'port' command not found. Please install MacPorts package from http://www.macports.org/install.php"
        exit 1
    fi

    if ! which python > /dev/null || which python | grep -q /usr/bin/python; then
        sudo port install python27
        sudo port select --set python python27
    fi

    if ! which pip > /dev/null; then
        sudo port install py-pip
        hash -r
    fi

    if ! which pip > /dev/null; then
        sudo port select --set pip pip27
        hash -r
    fi

    # this is optional, really
    #sudo pip install --upgrade pip
else
    echo "OS $OS is not supported"
    exit 1
fi

set +x

echo 'Setup Python packages...'

for f in $BIN/{..,.}/requirements.txt; do
    set -x
    sudo -H pip --trusted-host pypi.yandex-team.ru install --default-timeout=180 -r $f || true
    set +x
done

echo DONE
