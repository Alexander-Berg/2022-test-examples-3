conductor_pkgs:
  pkg.installed:
    - order: 0            #this hack is to make sure that we have all pkgs from Conductor installed before setting up host
    - pkgs:
      {% for package in salt['conductor.package']() %}
      - {{ package }}
      {% endfor %}
      - config-media-common
      - cauth-client-caching

monrun-regenerate:
  cmd.wait:
    - name: 'monrun --gen-jobs && /etc/init.d/snaked reconfigure'
    - cwd: /
