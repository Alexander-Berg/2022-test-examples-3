{% set cluster = pillar.get('cluster') %}

include:
  - units.rabbitmq-server
  - templates.certificates

/etc/yabs-chkdisk-stop:
  file.absent
    
