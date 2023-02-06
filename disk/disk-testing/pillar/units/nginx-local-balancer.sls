{% set unit = 'nginx-local-balancer' %}

# * Define "nginx-config-files" in your cluster or another pillar
#
#{{ unit }}-config-files:
#  - /etc/nginx/nginx.conf

{{ unit }}-config-files:
  common:
    basedir: "units/{{ unit }}/files"
    files:
      - /etc/nginx/conf.d/local_balancer_upstreams.conf
      - /etc/nginx/include/local_balancer_params.conf
      - /etc/nginx/sites-enabled/local_balancer.conf
