#!/bin/sh

OPTIND=1

STOCKS_AMOUNT=10000
VENDOR_AMOUNT=2
WAREHOUSE_AMOUNT=2

PUSH_STOCK_START="POST||/stocks"
FREEZE_STOCK_START="POST||/stocks/freeze"
CHECK_STOCK_START="POST||/stocks/checkAvailable"

show_help() {
cat << EOF
Usage: ${0##*/} -h [-s STOCKS_AMOUNT] [-w WAREHOUSE_AMOUNT] [-v VENDOR_AMOUNT]
Generate data for ammo generator for load test SS.
Final ammo amount = STOCKS_AMOUNT*WAREHOUSE_AMOUNT*VENDOR_AMOUNT
Generates:
push_stock_1.data - push stock with 1 sku
push_stock_5.data - push stock with 5 sku
push_stock_50.data - push stock with 50 sku
freeze_stock_1.data - freeze stock with 1 sku
freeze_stock_3_0.data - freeze stock with 3 unique sku
freeze_stock_3_2.data - freeze stock with 3 sku with 2 common sku in each ammo
freeze_push.data - freeze stock with 3 sku with 2 common sku in each ammo and push stock with 5 sku

    -h          display this help and exit
    -s          amount of stock for each vendor and warehouse. () DEFAULT: 10000
    -v          amount of vendors. DEFAULT: 2
    -w          amount of warehouses. DEFAULT: 2
EOF
}

generate_push_stock() {
    AMOUNT=$1
    if [ -z "$AMOUNT" ]; then
        AMOUNT=1
    fi

    AMMO_AMOUNT=$(((STOCKS_AMOUNT*VENDOR_AMOUNT*WAREHOUSE_AMOUNT)/AMOUNT))
    AMMO=0
    FILENAME="push_stock_"$AMOUNT".data"

    printf "\nGENERATE $FILENAME ($AMMO_AMOUNT)\n"
    progress $AMMO $AMMO_AMOUNT
    echo "" > $FILENAME

    for v in `seq 1 $VENDOR_AMOUNT`; do
        VENDOR_ID=$v
        for w in `seq 1 $WAREHOUSE_AMOUNT`; do
            let WAREHOUSE_ID=$w

            s=0
            while [ $s -lt $STOCKS_AMOUNT ]; do
                STOCKS_OBJ=""
                
                for s in `seq $((s+1)) $((s+AMOUNT))`; do
                    
                    let STOCK_ID=$s
                    if [ -n "$STOCKS_OBJ" ]; then
                        STOCKS_OBJ="$STOCKS_OBJ,"
                    fi    
                    
                    STOCKS_OBJ="$STOCKS_OBJ{\"unitId\": {\"vendorId\": $VENDOR_ID, \"article\": \"load-test-sku-$STOCK_ID\"}, \"warehouseId\": {\"yandexId\": $WAREHOUSE_ID, \"fulfillmentId\": $WAREHOUSE_ID }, \"stocks\": [{\"type\": 1, \"count\": 20000000$(( ( RANDOM % 100 ) )) }]}"
                done
                PUSH_STOCK_OBJ="{\"itemStocks\": [$STOCKS_OBJ]}"
                echo "$PUSH_STOCK_START||push_$AMOUNT||$PUSH_STOCK_OBJ" >> $FILENAME


                let AMMO=AMMO+1
                if [ $((s % 100)) -eq 0 ]; then
                   progress $AMMO $AMMO_AMOUNT
                fi
            done        
        done 
    done 

    cat $FILENAME | python ammo_generator.py > $FILENAME.ammo
    progress $AMMO $AMMO_AMOUNT
}

generate_freeze_stock() {
    AMOUNT=$1
    if [ -z "$AMOUNT" ]; then
        AMOUNT=1
    fi

    MIXED_AMOUNT=$2
    if [ -z "$MIXED_AMOUNT" ]; then
        MIXED_AMOUNT=0
    fi

    AMMO_AMOUNT=$(((STOCKS_AMOUNT*VENDOR_AMOUNT*WAREHOUSE_AMOUNT)/(AMOUNT-MIXED_AMOUNT)))
    AMMO=0

    FILENAME="freeze_stock_"$AMOUNT"_"$MIXED_AMOUNT".data"

    printf "\nGENERATE $FILENAME ($AMMO_AMOUNT) \n"
    progress $AMMO $AMMO_AMOUNT
    echo "" > $FILENAME

    for v in `seq 1 $VENDOR_AMOUNT`; do
        VENDOR_ID=$v
        for w in `seq 1 $WAREHOUSE_AMOUNT`; do
            let WAREHOUSE_ID=$w

            s=0
            while [ $s -lt $STOCKS_AMOUNT ]; do
                STOCKS_OBJ=""
                
                for s in `seq $((s+1)) $((s+AMOUNT))`; do
                    
                    let STOCK_ID=$s
                    if [ -n "$STOCKS_OBJ" ]; then
                        STOCKS_OBJ="$STOCKS_OBJ,"
                    fi    
                    
                    STOCKS_OBJ="$STOCKS_OBJ{\"sku\": \"load-test-sku-$STOCK_ID\", \"vendorId\": $VENDOR_ID, \"warehouseId\": $WAREHOUSE_ID, \"shopSku\": \"load-test-sku-$STOCK_ID\", \"quantity\": 1 }"
                done
                ORDER_ID=$(cat /dev/urandom | env LC_CTYPE=C tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
                FREEZE_STOCK_OBJ="{\"orderId\": \"load-test-order-$VENDOR_ID-$WAREHOUSE_ID-$ORDER_ID\", \"items\": [$STOCKS_OBJ]}"
                CHECK_STOCK_OBJ="{\"stocks\": [$STOCKS_OBJ]}"
                echo "$CHECK_STOCK_START||checkAvailable_$AMOUNT||$CHECK_STOCK_OBJ" >> $FILENAME
                echo "$FREEZE_STOCK_START||freeze_$AMOUNT||$FREEZE_STOCK_OBJ" >> $FILENAME

                let s=$s-$MIXED_AMOUNT
                let AMMO=AMMO+1
                if [ $((s % 100)) -eq 0 ]; then
                   progress $AMMO $AMMO_AMOUNT
                fi
            done        
        done 
    done 

    cat $FILENAME | python ammo_generator.py > $FILENAME.ammo
    progress $AMMO $AMMO_AMOUNT
}

generate_freeze_and_push_stock() {

    PUSH_AMOUNT=50
    FREEZE_AMOUNT=3
    MIXED_AMOUNT=2

    AMMO_AMOUNT=$(((STOCKS_AMOUNT*VENDOR_AMOUNT*WAREHOUSE_AMOUNT)/(FREEZE_AMOUNT-MIXED_AMOUNT)))
    AMMO=0

    FILENAME="push_"$PUSH_AMOUNT"_freeze_"$FREEZE_AMOUNT"_"$MIXED_AMOUNT"_stock.data"

    printf "\nGENERATE $FILENAME ($AMMO_AMOUNT)\n"
    progress $AMMO $AMMO_AMOUNT
    echo "" > $FILENAME

    for v in `seq 1 $VENDOR_AMOUNT`; do
        VENDOR_ID=$v
        for w in `seq 1 $WAREHOUSE_AMOUNT`; do
            let WAREHOUSE_ID=$w

            s=0
            while [ $s -lt $STOCKS_AMOUNT ]; do
                if [ $((s % 1000)) -eq 0 ]; then
                    STOCKS_OBJ=""
                    
                    for ps in `seq $((s+1)) $((s+PUSH_AMOUNT))`; do
                        
                        let STOCK_ID=$ps
                        if [ -n "$STOCKS_OBJ" ]; then
                            STOCKS_OBJ="$STOCKS_OBJ,"
                        fi    
                        
                        STOCKS_OBJ="$STOCKS_OBJ{\"unitId\": {\"vendorId\": $VENDOR_ID, \"article\": \"load-test-sku-$STOCK_ID\"}, \"warehouseId\": {\"yandexId\": $WAREHOUSE_ID, \"fulfillmentId\": $WAREHOUSE_ID }, \"stocks\": [{\"type\": 1, \"count\": 20000000$(( ( RANDOM % 100 ) )) }]}"
                    done
                    PUSH_STOCK_OBJ="{\"itemStocks\": [$STOCKS_OBJ]}"
                    echo "$PUSH_STOCK_START||push_$PUSH_AMOUNT||$PUSH_STOCK_OBJ" >> $FILENAME
                fi
               

                STOCKS_OBJ=""
                
                for s in `seq $((s+1)) $((s+FREEZE_AMOUNT))`; do
                    
                    let STOCK_ID=$s
                    if [ -n "$STOCKS_OBJ" ]; then
                        STOCKS_OBJ="$STOCKS_OBJ,"
                    fi    
                    
                    STOCKS_OBJ="$STOCKS_OBJ{\"sku\": \"load-test-sku-$STOCK_ID\", \"vendorId\": $VENDOR_ID, \"warehouseId\": $WAREHOUSE_ID, \"shopSku\": \"test-sku-$STOCK_ID\", \"quantity\": 1 }"
                done
                ORDER_ID=$(cat /dev/urandom | env LC_CTYPE=C tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
                FREEZE_STOCK_OBJ="{\"orderId\": \"load-test-order-$VENDOR_ID-$WAREHOUSE_ID-$ORDER_ID\", \"items\": [$STOCKS_OBJ]}"
                CHECK_STOCK_OBJ="{\"stocks\": [$STOCKS_OBJ]}"
                echo "$CHECK_STOCK_START||checkAvailable_$AMOUNT||$CHECK_STOCK_OBJ" >> $FILENAME
                echo "$CHECK_STOCK_START||checkAvailable_$AMOUNT||$CHECK_STOCK_OBJ" >> $FILENAME
                echo "$CHECK_STOCK_START||checkAvailable_$AMOUNT||$CHECK_STOCK_OBJ" >> $FILENAME
                echo "$CHECK_STOCK_START||checkAvailable_$AMOUNT||$CHECK_STOCK_OBJ" >> $FILENAME
                echo "$FREEZE_STOCK_START||freeze_$FREEZE_AMOUNT||$FREEZE_STOCK_OBJ" >> $FILENAME

                let s=$s-$MIXED_AMOUNT
                let AMMO=AMMO+1
                if [ $((s % 100)) -eq 0 ]; then
                   progress $AMMO $AMMO_AMOUNT
                fi
            done        
        done 
    done 

    cat $FILENAME | python ammo_generator.py > $FILENAME.ammo
    progress $AMMO $AMMO_AMOUNT
}

progress() {
    BAR=""
    PERCENT=$(awk "BEGIN { pc=100*${1}/${2}; i=int(pc); print (pc-i<0.5)?i:i+1 }")
    for p in `seq 0 $PERCENT`; do
        BAR="$BAR#"
    done
    printf '% -102s] (%s) AMMO:%s\r' [$BAR $PERCENT% $1 
}

while getopts "h?s:v:w:" opt; do
    case "$opt" in
        h|\?)
            show_help
            exit 0
            ;;
        s)  STOCKS_AMOUNT=$OPTARG
            ;;
        v)  VENDOR_AMOUNT=$OPTARG
            ;;
        w)  WAREHOUSE_AMOUNT=$OPTARG
            ;;
        *)
            show_help >&2
            exit 1
            ;;
    esac
done

shift $((OPTIND-1))
[ "$1" = "--" ] && shift


echo "STOCKS_AMOUNT=$STOCKS_AMOUNT, VENDOR_AMOUNT=$VENDOR_AMOUNT, WAREHOUSE_AMOUNT=$WAREHOUSE_AMOUNT Leftovers: $@"

generate_push_stock 1
generate_push_stock 5
generate_push_stock 50
generate_freeze_stock 1 0
generate_freeze_stock 3 0
generate_freeze_stock 3 2
generate_freeze_and_push_stock
