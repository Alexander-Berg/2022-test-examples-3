/etc/yandex/loggiver/loggiver.pattern:
  yafile.managed:
    - source: salt://files/{{pillar.get('cluster')}}/etc/yandex/loggiver/loggiver.pattern
    - template: jinja
    - user: root
    - group: root
    - mode: 644
    - makedirs: True

loggiver:
  pkg.installed:
    - pkgs: {{ salt['conductor.package']('yandex-3132-fastcgi-loggiver') }}
  service.running:
    - require:
      - pkg: loggiver


