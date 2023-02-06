graphite:
  -
    period: 60
    timeunit: 'SECONDS'
    prefix: media.disk.[__C_GROUP__].[__HOSTNAME__]
    hosts:
     - host: 'localhost'
       port: 42000
    predicate:
      color: "white"
      useQualifiedName: true
      patterns:
        - "^org.apache.cassandra.metrics.+"
