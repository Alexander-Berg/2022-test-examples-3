export PYTHONUNBUFFERED=1
export Y_PYTHON_SOURCE_ROOT=$HOME"/work/arc/arcadia"

#export RASP_FORCE_NO_FLAG=1
#export RASP_TESTS_SKIP_INIT=1
#export RASP_TESTS_SKIP_BI=1
#export RASP_SWITCH_CONTINUE=1
#export DJANGO_SETTINGS_MODULE=travel.rasp.admin.tests_integration.local_settings_mdb


export SANDBOX_OAUTH_TOKEN=$(cat $HOME"/.sandbox_oauth")
export RASP_MDB_OAUTH_TOKEN=$(cat $HOME"/.mdb_oauth")

export RASP_TESTS_MYSQL_NAME_MAINTENANCE=rasp_integration_maintenance
export RASP_TESTS_MYSQL_HOST_MAINTENANCE=sas-rwu5kzgpd1h29he9.db.yandex.net
export RASP_TESTS_MYSQL_PORT_MAINTENANCE=3306
export RASP_TESTS_MYSQL_USER_MAINTENANCE=rasp_test
export RASP_TESTS_MYSQL_PASSWORD_MAINTENANCE=7nzEeA4aM4pJ4Mbp

export RASP_TESTS_MYSQL_NAME_DB1=rasp_integration_db1
export RASP_TESTS_MYSQL_HOST_DB1=sas-6clw5cqj18ilfjvc.db.yandex.net
export RASP_TESTS_MYSQL_PORT_DB1=3306
export RASP_TESTS_MYSQL_USER_DB1=rasp_test
export RASP_TESTS_MYSQL_PASSWORD_DB1=7nzEeA4aM4pJ4Mbp

export RASP_TESTS_MYSQL_NAME_DB2=rasp_integration_db2
export RASP_TESTS_MYSQL_HOST_DB2=sas-ghvi9a0wa8vpvm8p.db.yandex.net
export RASP_TESTS_MYSQL_PORT_DB2=3306
export RASP_TESTS_MYSQL_USER_DB2=rasp_test
export RASP_TESTS_MYSQL_PASSWORD_DB2=7nzEeA4aM4pJ4Mbp

#export PYTHONIOENCODING=utf_8

#REQUESTS_CA_BUNDLE=/usr/local/share/ca-certificates/YandexInternalRootCA.crt "$@"

export RASP_INFLECTOR_URL=http://reqwizard.yandex.net:8891/wizard

"$@"
