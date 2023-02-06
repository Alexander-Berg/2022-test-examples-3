#!/bin/bash
set -eo pipefail

HOST="ui-1.ui.loadtest.fan-back.mail.stable.qloud-d.yandex.net"
PORT=8800
BASE_URL="$HOST:$PORT"

# see fan_ui/settings/095-tvm.conf.loadtest for details
X_YA_SERVICE_TICKET="3:serv:CBAQ__________9_IgUIexDIAw:U3oDh3-UV6sKSHcQR4HHTLoswwzw6DQn_GPQ0wvWEFJexSMRGrUsFiF-u_QrjPEbM0sCQaWp3wpiTrM8NKE_OepKiHSrs5VPXGpu92KhdSfJeK82sEWKMZeoR4P7yTZWf8VJ_tj16q36eQOBGLaZICBU54O4evQCwQFnkYRxE38"
X_YA_SERVICE_TICKET_HEADER="X-Ya-Service-Ticket:$X_YA_SERVICE_TICKET"

CONCURRENCY="$1"
if [[ -z "$CONCURRENCY" ]]; then
    CONCURRENCY=4
fi

declare -A methods=(
    ["/api/v1/campaign-list"]="user_id=1&account_slug=load"
    ["/api/v1/campaign"]="user_id=1&account_slug=load&campaign_slug=8S77C5Z3-PUB"
    ["/api/v1/campaign-details"]="user_id=1&account_slug=load&campaign_slug=8S77C5Z3-PUB"
    ["/api/v1/campaign-maillist"]="user_id=1&account_slug=load&campaign_slug=8S77C5Z3-PUB&filename=maillist_20000.csv"
    ["/api/v1/campaign-letter"]="user_id=1&account_slug=load&campaign_slug=8S77C5Z3-PUB&filename=letter.zip"
    ["/api/v1/org-limits"]="user_id=1&org_id=1"
    ["/api/v1/org-domain-list"]="user_id=1&org_id=1"
    ["/api/v1/test-send-task"]="user_id=1&account_slug=load&campaign_slug=4H6N3QH4-YOL"
    ["/api/send/campaign-recipient-list"]="account_slug=load&campaign_slug=8S77C5Z3-PUB"
)

declare -A args=(
    ["/api/v1/campaign-list"]="-H $X_YA_SERVICE_TICKET_HEADER"
    ["/api/v1/campaign"]="-H $X_YA_SERVICE_TICKET_HEADER"
    ["/api/v1/campaign-details"]="-u details-payload.json -T application/json -H $X_YA_SERVICE_TICKET_HEADER"
    ["/api/v1/campaign-maillist"]="-u ../../pylib/fan/tests/data/csv/maillist_20000.csv -T text/csv -H $X_YA_SERVICE_TICKET_HEADER"
    ["/api/v1/campaign-letter"]="-u letter.zip -T application/zip -H $X_YA_SERVICE_TICKET_HEADER"
    ["/api/v1/org-limits"]="-H $X_YA_SERVICE_TICKET_HEADER"
    ["/api/v1/org-domain-list"]="-H $X_YA_SERVICE_TICKET_HEADER"
    ["/api/v1/test-send-task"]="-p test-send-task.json -T application/json -H $X_YA_SERVICE_TICKET_HEADER"
    ["/api/send/campaign-recipient-list"]=""
)


for m in "${!methods[@]}"; do
    echo "Shooting $m..."
    ab -k -d -q -c "$CONCURRENCY" -t 180 ${args[$m]} "${BASE_URL}${m}?${methods[$m]}" |
        fgrep -e "Complete requests" -e "Failed requests" -e "Non-2xx responses" -e "Requests per second" -e "Time per request" -e "Document Path" -e "Document Length"
    echo
done
