# intermediate='//home/market/development/replenishment/andrleew/REPLENISHMENT-3633/new_warehouse/'
# outdir='test_new_region/test_data/'
execute_name=$0

help(){
    echo "script for reading intermediate tables"
    echo "${execute_name} -i <intermediate yt path> --o <dir to write tables>"
}

while [[ $# -gt 0 ]]
    do
        key="$1"

        case $key in
            -i|--in)
                intermediate=$2
                shift
                shift
                ;;
            -o|--out)
                outdir=$2
                shift
                shift
                ;;
            -h|--help)
                help
                exit 0
                ;;
        esac
    done
        

tables=(abc assortment delivery_limit_groups delivery_options forecast_region manual_stock_model msku_delivery_options msku_info orders_alpaca prices_alpaca regions regional_assortment ss_region ssku_mappings stock_alpaca stock_with_lifetime suppliers transits warehouses)
# declare -A mv_tables=( [forecast_region]=forecast [orders_alpaca]=orders [prices_alpaca]=prices [ss_region]=safety_stock [stock_alpaca]=stock )

format=yson

mkdir -p $outdir
for table in ${tables[@]}
    do
        echo $table
        yt read-table --format ${format} "${intermediate}${table}" > "${outdir}${table}.${format}"
    done

# for table in "${!mv_tables[@]}"
#    do
#        cp "${outdir}${table}.${format}" "${outdir}${mv_tables[${table}]}.${format}"
#    done
