cluster: disk_queue

include:
  - units.rabbitmq-server

rabbitmq-server-vhosts:
  - mqueue-vhost
  - mqueue-vhost-current
  - mqueue-vhost-manual
  - mqueue-vhost-stable
  - mqueue-vhost-hotfix
 

rabbitmq-server-users:
  mqueue-user:
{#    password: ololo #}
    tags: 
      - management
    perms:
      mqueue-vhost:
        - '.*'
        - '.*'
        - '.*'
      mqueue-vhost-current:
        - '.*'
        - '.*'
        - '.*'
      mqueue-vhost-manual:
        - '.*'
        - '.*'
        - '.*'
      mqueue-vhost-stable:
        - '.*'
        - '.*'
        - '.*'
      mqueue-vhost-hotfix:
        - '.*'
        - '.*'
        - '.*'




rabbitmq-server-plugins:
  - management

rabbitmq-server-dirs:
  disk_mqueue:
    dirs:
      - /u0/rabbitmq

certificates:
  contents:
    rabbitmq-vhost-access.yaml: {{ salt.yav.get('sec-01crz2rtbqyg06fykcpa42s5rd[rabbitmq-vhost-access.yaml]') | json }}
  path: /etc/monitoring/
  packages: []
  cert_owner: www-data

