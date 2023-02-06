'search-trusty-testing-all':
    pkgrepo.managed:
        - refresh: false
        - watch_in:
            - cmd: apt-get-qq-update
        - require_in:
            - cmd: apt-get-qq-update
        - name: 'deb http://dist.yandex.ru/search-trusty testing/all/'
        - file: /etc/apt/sources.list.d/search-trusty-testing.list

'search-trusty-testing-arch':
    pkgrepo.managed:
        - refresh: false
        - watch_in:
            - cmd: apt-get-qq-update
        - require_in:
            - cmd: apt-get-qq-update
        - name: 'deb http://dist.yandex.ru/search-trusty testing/$(ARCH)/'
        - file: /etc/apt/sources.list.d/search-trusty-testing.list

include:
    - components.repositories.apt

