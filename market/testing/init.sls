confd:
  datasources:
    oracle__tnsnames.ora:
      ks:
        - "/testing/oracle/tnsnames.ora/"
      src: tnsnames-ora.tmpl
      dest: /etc/oracle/tnsnames.ora
    billing__billing.properties:
      prefix: /datasources/testing/yandex/market-datasources/billing/billing.properties
      dest: /etc/yandex/market-datasources/billing/billing.properties
    clicks-indexer.conf:
      ks:
        - "/testing/yandex/market-datasources/clicks-indexer.conf/"
      src: subdir-config-file.tmpl
      dest: /etc/yandex/market-datasources/clicks-indexer.conf
    datasources.properties:
      ks:
        - "/testing/yandex/market-datasources/datasources.properties/"
        - "/testing/yandex/market-datasources/picrobot.cfg/"
        - "/ir/testing/"
        - "/checkout/testing/"
        - "/marketcms/testing/"
        - "/mbo/testing/"
        - "/mboc/testing/"
        - "/lgw/testing/"
        - "/redmarket/testing/"
        - "/pers/testing/"
        - "/mdb/testing/"
        - "/marketpromo/testing/"
        - "/deepmind/testing/"
      src: datasources.properties.tmpl
      dest: /etc/yandex/market-datasources/datasources.properties
    datasources.sh:
      prefix: /datasources/testing/yandex/market-datasources/datasources.sh
      dest: /etc/yandex/market-datasources/datasources.sh
    datasources.xml:
      ks:
        - "/testing/yandex/market-datasources/datasources.xml/"
      src: datasources.xml.tmpl
      dest: /etc/yandex/market-datasources/datasources.xml
    experiments-fcgi.cfg:
      ks:
        - "/testing/yandex/market-datasources/experiments-fcgi.cfg/"
      src: config-file.tmpl
      dest: /etc/yandex/market-datasources/experiments-fcgi.cfg
    indexer__planeshift.stratocaster.conf:
      ks:
        - "/testing/yandex/market-datasources/indexer/planeshift.stratocaster.conf/"
      src: subdir-config-file.tmpl
      dest: /etc/yandex/market-datasources/indexer/planeshift.stratocaster.conf
    indexer__stratocaster.conf:
      ks:
        - "/testing/yandex/market-datasources/indexer/stratocaster.conf/"
      src: subdir-config-file.tmpl
      dest: /etc/yandex/market-datasources/indexer/stratocaster.conf
    indexer__red.stratocaster.conf:
      ks:
        - "/testing/yandex/market-datasources/indexer/red.stratocaster.conf/"
      src: subdir-config-file.tmpl
      dest: /etc/yandex/market-datasources/indexer/red.stratocaster.conf
    indexer__fresh.stratocaster.conf:
      ks:
        - "/testing/yandex/market-datasources/indexer/fresh.stratocaster.conf/"
      src: subdir-config-file.tmpl
      dest: /etc/yandex/market-datasources/indexer/fresh.stratocaster.conf
    indexer__turbo.gibson.conf:
      ks:
        - "/testing/yandex/market-datasources/indexer/turbo.gibson.conf/"
      src: subdir-config-file.tmpl
      dest: /etc/yandex/market-datasources/indexer/turbo.gibson.conf
    marketstat.properties:
      prefix: /datasources/testing/yandex/market-datasources/marketstat.properties
      dest: /etc/yandex/market-datasources/marketstat.properties
#    marketuslug.conf:
#      ks:
#        - "/testing/yandex/market-datasources/marketuslug.conf/"
#      src: yandex__market-datasources__marketuslug.conf.tmpl
#      dest: /etc/yandex/market-datasources/marketuslug.conf
    mbi-endpoints.ent:
      ks:
        - "/testing/yandex/market-datasources/mbi-endpoints.ent/"
      src: mbi-endpoints.ent.tmpl
      dest: /etc/yandex/market-datasources/mbi-endpoints.ent
    mbo__mbo-search.properties:
      prefix: /datasources/testing/yandex/market-datasources/mbo/mbo-search.properties
      dest: /etc/yandex/market-datasources/mbo/mbo-search.properties
    mbo__solr-env-stratocaster.sh:
      prefix: /datasources/testing/yandex/market-datasources/mbo/solr-env-stratocaster.sh
      dest: /etc/yandex/market-datasources/mbo/solr-env-stratocaster.sh
    mbo__solr-env-telecaster.sh:
      prefix: /datasources/testing/yandex/market-datasources/mbo/solr-env-telecaster.sh
      dest: /etc/yandex/market-datasources/mbo/solr-env-telecaster.sh
    mstat-bot.yaml:
      ks:
        - "/testing/yandex/market-datasources/mstat-bot.yaml/"
      src: yaml.tmpl
      dest: /etc/yandex/market-datasources/mstat-bot.yaml
    picrobot.cfg:
      ks:
        - "/testing/yandex/market-datasources/picrobot.cfg/"
      src: config-file.tmpl
      dest: /etc/yandex/market-datasources/picrobot.cfg
    planeshift.stratocaster__datasources.properties:
      prefix: /datasources/testing/yandex/market-datasources/planeshift.stratocaster/datasources.properties
      dest: /etc/yandex/market-datasources/planeshift.stratocaster/datasources.properties
    super-controller__planeshift.stratocaster.properties:
      prefix: /datasources/testing/yandex/market-datasources/super-controller/planeshift.stratocaster.properties
      dest: /etc/yandex/market-datasources/super-controller/planeshift.stratocaster.properties
    super-controller__stratocaster.properties:
      prefix: /datasources/testing/yandex/market-datasources/super-controller/stratocaster.properties
      dest: /etc/yandex/market-datasources/super-controller/stratocaster.properties
    super-controller__superstrat.properties:
      prefix: /datasources/testing/yandex/market-datasources/super-controller/superstrat.properties
      dest: /etc/yandex/market-datasources/super-controller/superstrat.properties
    super-controller__telecaster.properties:
      prefix: /datasources/testing/yandex/market-datasources/super-controller/telecaster.properties
      dest: /etc/yandex/market-datasources/super-controller/telecaster.properties
    yarn__hadoop-metrics.properties:
      prefix: /datasources/testing/yandex/market-datasources/yarn/hadoop-metrics.properties
      dest: /etc/yandex/market-datasources/yarn/hadoop-metrics.properties
    yarn__hadoop-metrics2.properties:
      prefix: /datasources/testing/yandex/market-datasources/yarn/hadoop-metrics2.properties
      dest: /etc/yandex/market-datasources/yarn/hadoop-metrics2.properties
    yarn__log4j.properties:
      prefix: /datasources/testing/yandex/market-datasources/yarn/log4j.properties
      dest: /etc/yandex/market-datasources/yarn/log4j.properties
    yt-sender.yaml:
      ks:
        - "/testing/yandex/market-datasources/yt-sender.yaml/"
      src: yaml.tmpl
      dest: /etc/yandex/market-datasources/yt-sender.yaml
    zookeeper__planeshift.stratocaster.conf:
      ks:
        - "/testing/yandex/market-datasources/zookeeper/planeshift.stratocaster.conf/"
      src: subdir-config-file.tmpl
      dest: /etc/yandex/market-datasources/zookeeper/planeshift.stratocaster.conf
    zookeeper__stratocaster.conf:
      ks:
        - "/testing/yandex/market-datasources/zookeeper/stratocaster.conf/"
      src: subdir-config-file.tmpl
      dest: /etc/yandex/market-datasources/zookeeper/stratocaster.conf
    zookeeper__telecaster.conf:
      ks:
        - "/testing/yandex/market-datasources/zookeeper/telecaster.conf/"
      src: subdir-config-file.tmpl
      dest: /etc/yandex/market-datasources/zookeeper/telecaster.conf
    zookeeper__fresh.stratocaster.conf:
      ks:
        - "/testing/yandex/market-datasources/zookeeper/fresh.stratocaster.conf/"
      src: subdir-config-file.tmpl
      dest: /etc/yandex/market-datasources/zookeeper/fresh.stratocaster.conf
    pers-history.properties:
       prefix: /datasources/testing/yandex/market-datasources/local/pers-history.properties.d/cs_all
       dest: /etc/yandex/market-datasources/local/pers-history.properties
    market-carter.properties:
      prefix: /datasources/testing/yandex/market-datasources/local/market-carter.properties.d/cs_all
      dest: /etc/yandex/market-datasources/local/market-carter.properties
    market-notifier.environment:
      prefix: /datasources/testing/yandex/market-datasources/local/market-notifier.environment.d/cs_all
      dest: /etc/yandex/market-datasources/local/market-notifier.environment
    market-mailer.properties:
      prefix: /datasources/testing/yandex/market-datasources/local/market-mailer.properties.d/cs_all
      dest: /etc/yandex/market-datasources/local/market-mailer.properties
    market-utils.properties:
      prefix: /datasources/testing/yandex/market-datasources/local/market-utils.properties.d/cs_all
      dest: /etc/yandex/market-datasources/local/market-utils.properties
