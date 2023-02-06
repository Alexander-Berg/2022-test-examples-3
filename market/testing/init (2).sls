etcd:
  users:
    monitoring:
      password: {{ salt["yav.get"]("sec-01fz0wv6xq1h3qtdp64gkyrw0m[testing-monitoring-password]") }}
      paths:
        - '/_monitoring': readwrite
