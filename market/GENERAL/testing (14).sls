stunnel:
  config:
    services:
      - name: ldaps
        client: 'yes'
        accept: '127.0.0.1:636'
        connect: '141.8.146.19:636'
