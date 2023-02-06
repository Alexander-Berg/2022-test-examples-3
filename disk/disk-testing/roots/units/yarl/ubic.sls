/usr/share/perl5/Ubic/Service/YARL.pm:
  file.managed:
    - source: salt://{{ slspath }}/files/ubic/YARL.pm
    - mode: 644
    - makedirs: True
    - require:
      - cmd: generate_config

/etc/ubic/service/yarl.json:
  file.managed:
    - source: salt://{{ slspath }}/files/ubic/yarl.json
    - mode: 644
    - makedirs: True
    - require:
       - file: /usr/share/perl5/Ubic/Service/YARL.pm
       - file: /etc/init.d/yarl

/etc/init.d/yarl:
  file.managed:
    - source: salt://{{ slspath }}/files/ubic/yarl
    - mode: 755
    - makedirs: True
