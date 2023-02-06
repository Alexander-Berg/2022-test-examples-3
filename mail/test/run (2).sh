export TEST_XCONF_CONNINFO="host=sas-ommek17z8j1rx2a4.db.yandex.net,vla-xxgmbzz6dya5kuun.db.yandex.net \
      port=6432 \
      sslmode=verify-full \
      dbname=xconf_dev \
      user=xiva_user \
      target_session_attrs=read-write"

pytest .
