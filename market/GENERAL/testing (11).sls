mysql:
  databases:
    - checkout_archive
    - checkout_perf
  users:
    carter:
      password: {{ salt["yav.get"]("sec-01fz0tracds40kdcdbemw6ysbs[testing-carter-password]") }}
      host: "%"
    checkouter:
      password: {{ salt["yav.get"]("sec-01fz0tracds40kdcdbemw6ysbs[testing-checkouter-password]") }}
      host: "%"
    monitor:
      password: {{ salt["yav.get"]("sec-01fz0tracds40kdcdbemw6ysbs[testing-monitor-password]") }}
      host: "localhost"
    notifier:
      password: {{ salt["yav.get"]("sec-01fz0tracds40kdcdbemw6ysbs[testing-notifier-password]") }}
      host: "%"
    repl:
      password: {{ salt["yav.get"]("sec-01fz0tracds40kdcdbemw6ysbs[testing-repl-password]") }}
      host: "%.market.yandex.net"
  grants:
    carter_archive:
      grant: all privileges
      database: checkout_archive.*
      user: carter
      host: "%"
    carter_perf:
      grant: all privileges
      database: checkout_perf.*
      user: carter
      host: "%"
    checkouter_archive:
      grant: all privileges
      database: checkout_archive.*
      user: checkouter
      host: "%"
    checkouter_perf:
      grant: all privileges
      database: checkout_perf.*
      user: checkouter
      host: "%"
    notifier_archive:
      grant: all privileges
      database: checkout_archive.*
      user: notifier
      host: "%"
    notifier_perf:
      grant: all privileges
      database: checkout_perf.*
      user: notifier
      host: "%"
    monitor:
      grant: process, replication client
      database: "*.*"
      user: monitor
      host: localhost
    repl:
      grant: replication slave
      database: "*.*"
      user: repl
      host: "%.market.yandex.net"
