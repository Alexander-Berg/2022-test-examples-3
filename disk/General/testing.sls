'common-testing-all':
    pkgrepo.managed:
        - refresh: false
        - watch_in:
            - cmd: apt-get-qq-update
        - require_in:
            - cmd: apt-get-qq-update
        - name: 'deb http://dist.yandex.ru/common testing/all/'
        - file: /etc/apt/sources.list.d/common-testing.list
        - require_in:
            - pkgrepo: common-stable-all
            - pkgrepo: common-stable-arch

'common-testing-arch':
    pkgrepo.managed:
        - refresh: false
        - watch_in:
            - cmd: apt-get-qq-update
        - require_in:
            - cmd: apt-get-qq-update
        - name: 'deb http://dist.yandex.ru/common testing/$(ARCH)/'
        - file: /etc/apt/sources.list.d/common-testing.list
        - require_in:
            - pkgrepo: common-stable-all
            - pkgrepo: common-stable-arch

include:
    - components.repositories.apt

