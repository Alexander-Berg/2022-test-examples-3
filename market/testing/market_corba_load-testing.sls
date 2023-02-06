confd:
  datasources:
    pers-history.properties:
      prefix: /datasources/testing/yandex/market-datasources/local/pers-history.properties.d/market_corba_load-testing
      dest: /etc/yandex/market-datasources/local/pers-history.properties
    market-carter.properties:
      prefix: /datasources/testing/yandex/market-datasources/local/market-carter.properties.d/market_corba_load-testing
      dest: /etc/yandex/market-datasources/local/market-carter.properties
    market-notifier.environment:
      prefix: /datasources/testing/yandex/market-datasources/local/market-notifier.environment.d/market_corba_load-testing
      dest: /etc/yandex/market-datasources/local/market-notifier.environment
    market-mailer.properties:
      prefix: /datasources/testing/yandex/market-datasources/local/market-mailer.properties.d/market_corba_load-testing
      dest: /etc/yandex/market-datasources/local/market-mailer.properties
    market-utils.properties:
      prefix: /datasources/testing/yandex/market-datasources/local/market-utils.properties.d/market_corba_load-testing
      dest: /etc/yandex/market-datasources/local/market-utils.properties
    pers-static.properties:
      prefix: /datasources/testing/yandex/market-datasources/local/pers-static.properties.d/market_corba_load-testing
      dest: /etc/yandex/market-datasources/local/pers-static.properties
