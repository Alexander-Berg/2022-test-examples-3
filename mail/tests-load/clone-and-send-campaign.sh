#!/bin/bash

FAN_BASE_URL="ui-1.ui.loadtest.fan-back.mail.stable.qloud-d.yandex.net:8800/api/v1"

# see fan_ui/settings/095-tvm.conf.loadtest for details
X_YA_SERVICE_TICKET="3:serv:CBAQ__________9_IgUIexDIAw:U3oDh3-UV6sKSHcQR4HHTLoswwzw6DQn_GPQ0wvWEFJexSMRGrUsFiF-u_QrjPEbM0sCQaWp3wpiTrM8NKE_OepKiHSrs5VPXGpu92KhdSfJeK82sEWKMZeoR4P7yTZWf8VJ_tj16q36eQOBGLaZICBU54O4evQCwQFnkYRxE38"

USER_ID="1"
ACCOUNT_SLUG="load"
SOURCE_CAMPAIGN_SLUG="WC023UH4-2YH1"

echo "Cloning campaign $SOURCE_CAMPAIGN_SLUG..."

campaign_slug=$(curl -X POST \
    -H "X-Ya-Service-Ticket: $X_YA_SERVICE_TICKET" \
    "$FAN_BASE_URL/campaign"`
        `"?user_id=$USER_ID"`
        `"&account_slug=$ACCOUNT_SLUG"`
        `"&source_campaign_slug=$SOURCE_CAMPAIGN_SLUG" 2> /dev/null | jq '.slug' | sed --expression='s/"//g')

echo "Sending cloned campaign $campaign_slug..."

curl -X POST \
    -H "X-Ya-Service-Ticket: $X_YA_SERVICE_TICKET" \
    "$FAN_BASE_URL/campaign-state"`
        `"?user_id=$USER_ID"`
        `"&account_slug=$ACCOUNT_SLUG"`
        `"&campaign_slug=$campaign_slug"`
        `"&state=sending"
