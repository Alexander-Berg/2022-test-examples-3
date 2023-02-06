#!/bin/sh
# Скрипт стреляет http-запросами по серверу
# Из файла берутся запросы (патроны), которые отправляются на сервер. Кол-во параллельных запросов - 10 (по умолчанию)
# Пример патрона
#   /v1/model/116335/offers.xml?geo_id=213&ip=::ffff:89.108.70.131&page=1&region_id=213&sort=price&count=30&delivery=0&operatorid=99&countrycode=255&signalstrength=0&uuid=56ce13c1f2142fabe20bcdf62b326fa9
# Готовые патроны можно взять на braavos:/home/zoom/var/testdata

# Файл откуда берутся патроны
FILE=$1

if [[ -z "${FILE}" ]]; then
  echo "File is not specified"
  exit 1
fi

# Максимальное кол-во параллельных запросов
MAX_JOBS=10

# Хост, в который будет стрельба
HOST=gravicapa2ft:34824
#HOST=aida:40102
#HOST=braavos:40102

# Подставляемые параметры для каждого патрона
OAUTH_TOKEN=4c297f263cc949fa8e0178b1382d3822
SECRET=5zaAy1Y9J3AV3vyXw9UpyCpSS0IYes

_curl() {
  LINE="$1"
  REQUEST="http://${HOST}${LINE}"
#  printf "\n\n"
#  echo "Request: ${REQUEST}"
#  printf "\n"
  curl -m 10 --silent -H "Authorization: ${SECRET}" "${REQUEST}&oauth_token=${OAUTH_TOKEN}" > /dev/null 2>&1  &
}

wait_for_free_job_slot() {
  JOB_COUNT=$(jobs -l | wc -l)
  while [[ ${JOB_COUNT} -ge ${MAX_JOBS} ]]
  do
    sleep 0.001
    JOB_COUNT=$(jobs -l | wc -l)
  done
}

while read LINE
do
  _curl "${LINE}"
  wait_for_free_job_slot
done < ${FILE}

echo "Finished."
exit 0
