'yandex-trusty-testing-all':
    pkgrepo.managed:
        - refresh: false
        - watch_in:
            - cmd: apt-get-qq-update
        - require_in:
            - cmd: apt-get-qq-update
        - name: 'deb http://dist.yandex.ru/yandex-trusty testing/all/'
        - file: /etc/apt/sources.list.d/yandex-trusty-testing.list

'yandex-trusty-testing-arch':
    pkgrepo.managed:
        - refresh: false
        - watch_in:
            - cmd: apt-get-qq-update
        - require_in:
            - cmd: apt-get-qq-update
        - name: 'deb http://dist.yandex.ru/yandex-trusty testing/$(ARCH)/'
        - file: /etc/apt/sources.list.d/yandex-trusty-testing.list

include:
    - components.repositories.apt

