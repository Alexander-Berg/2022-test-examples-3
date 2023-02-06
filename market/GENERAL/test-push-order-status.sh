#!/bin/bash

if [ -z "$1" ]; then
    echo -n "Enter order id: "
    read ORDERID
else
    ORDERID=$1
fi

PWD=$(dirname $0)
source "$PWD/other/parse_env.sh"
source "$PWD/tickets/get_service_ticket.sh"


curl -X POST "$CHECKOUT_URL/ds/push-order-statuses-changed" \
-H "Accept: text/xml" \
-H "Content-Type: text/xml" \
-H "X-Ya-Service-Ticket: $SERVICE_TICKET" \
--data "<root>
    <token>ZGvnWWInQvbSpx8Tqi4AcnkNDC6AZvmMmZjTzVYaYlY0Ii1Hr7L007J0miR8n9bw</token>
    <uniq>456</uniq>
    <hash>789</hash>
    <request type=\"pushOrdersStatusesChanged\">
        <ordersIds>
            <orderId>
                <yandexId>$ORDERID</yandexId>
            </orderId>
        </ordersIds>
    </request>
</root>"

echo ""
