#!/bin/bash

cp apps/setup.py ./

for pkg_type in api disk queue; do
    # Remove debian directory if exists
    rm -rf debian
    cp -R apps/${pkg_type}/deploy/debian ./
    LASTVERSION=$(dpkg-parsechangelog --show-field Version)
    VERSION=$LASTVERSION-$(svn info | grep Revision | cut -c11-)-development
    echo "##teamcity[buildNumber '$VERSION']"
    dch -b -v $VERSION "Build for testing"
    debuild --no-tgz-check -i -us -uc -b
    tools/upload_to_sandbox.py $pkg_type $VERSION
done
