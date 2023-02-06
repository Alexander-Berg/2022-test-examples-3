base:
  pkgrepo.managed:
    - humanname: ubuntu-toolchain-r /test/ubuntu ppa
    - name: deb http://ppa.launchpad.net/ubuntu-toolchain-r/test/ubuntu trusty main
    - dist: trusty
    - file: /etc/apt/sources.list.d/ubuntu-toolchain-r.list
    - keyid: BA9EF27F
    - keyserver: keyserver.ubuntu.com
