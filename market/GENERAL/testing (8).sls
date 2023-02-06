clickhouse:
  users:
    clickphite:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-clickphite-password]").strip() }}"
    ro:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-ro-password]").strip() }}"
    default:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-default-password]").strip() }}"
    readonly:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-readonly-password]").strip() }}"
    delivery:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-delivery-password]").strip() }}"
    logshatter:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-logshatter-password]").strip() }}"
    internal:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-internal-password]").strip() }}"
    clickphite_rw:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-clickphite_rw-password]").strip() }}"
    ir_dev:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-ir_dev-password]").strip() }}"
    mbologs:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-mbologs-password]").strip() }}"
    content_api:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-content_api-password]").strip() }}"
    testers_clicklog:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-testers_clicklog-password]").strip() }}"
    partner_api:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-partner_api-password]").strip() }}"
    marketstat:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-marketstat-password]").strip() }}"
    tsum:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-tsum-password]").strip() }}"
    graphite:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-graphite-password]").strip() }}"
    pricecenter:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-pricecenter-password]").strip() }}"
    statface:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-statface-password]").strip() }}"
    distribution:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-distribution-password]").strip() }}"
    public:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-public-password]").strip() }}"
    market_abo:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-market_abo-password]").strip() }}"
    di:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-di-password]").strip() }}"
    marketstat_tm:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-marketstat_tm-password]").strip() }}"
    pricelabs_api:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-pricelabs_api-password]").strip() }}"
    pricechart:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-pricechart-password]").strip() }}"
    lunapark:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-lunapark-password]").strip() }}"
    dealer:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-dealer-password]").strip() }}"
    yql:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-yql-password]").strip() }}"
    datalens:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-datalens-password]").strip() }}"
    robot_market_daas:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-robot_market_daas-password]").strip() }}"
    csadmin:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-csadmin-password]").strip() }}"
    checkouter:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-checkouter-password]").strip() }}"
    market-cubes:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-market-cubes-password]").strip() }}"
    market-ir-ro:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-market-ir-ro-password]").strip() }}"
    yandex-market-antifraud-online:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-yandex-market-antifraud-online-password]").strip() }}"
    antifraud:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-antifraud-password]").strip() }}"
    checkouter-solomon-pusher:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-checkouter-solomon-pusher-password]").strip() }}"
    pers-content-rank:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-pers-content-rank-password]").strip() }}"
    antirobot_report:
      password: "{{ salt["yav.get"]("sec-01fz0w72x39d3a8vmehqezc4eh[testing-antirobot_report-password]").strip() }}"
