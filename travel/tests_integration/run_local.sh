export PYTHONUNBUFFERED=1
export Y_PYTHON_SOURCE_ROOT=$HOME"/work/arc/arcadia"

#export RASP_TESTS_SKIP_INIT=1
#export RASP_FORCE_NO_FLAG=1

#export RASP_TESTS_SKIP_BI=1
#export RASP_BI_CONTINUE=1

#export RASP_SWITCH_CONTINUE=1

export SANDBOX_OAUTH_TOKEN=$(cat $HOME"/.sandbox_oauth")
export RASP_MDB_OAUTH_TOKEN=$(cat $HOME"/.mdb_oauth")

export RASP_INFLECTOR_URL=http://reqwizard.yandex.net:8891/wizard

"$@"
