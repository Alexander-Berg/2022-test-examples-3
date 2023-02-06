'mail-trusty-testing-all':
    pkgrepo.managed:
        - refresh: false
        - watch_in:
            - cmd: apt-get-qq-update
        - require_in:
            - cmd: apt-get-qq-update
        - name: 'deb http://dist.yandex.ru/mail-trusty testing/all/'
        - file: /etc/apt/sources.list.d/mail-trusty-testing.list
        - require_in:
            - pkgrepo: mail-trusty-stable-all
            - pkgrepo: mail-trusty-stable-arch

'mail-trusty-testing-arch':
    pkgrepo.managed:
        - refresh: false
        - watch_in:
            - cmd: apt-get-qq-update
        - require_in:
            - cmd: apt-get-qq-update
        - name: 'deb http://dist.yandex.ru/mail-trusty testing/$(ARCH)/'
        - file: /etc/apt/sources.list.d/mail-trusty-testing.list
        - require_in:
            - pkgrepo: mail-trusty-stable-all
            - pkgrepo: mail-trusty-stable-arch

include:
    - components.repositories.apt

