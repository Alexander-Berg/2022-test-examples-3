#!/bin/bash

shopt -s expand_aliases
irrel_thes=0.14 # transactions marked lower this theshold are irrel
rel_thes=0.6
current_default_thres=0.033
#default_thres=0.015
###
# run default & offline models on test part of offline pool
###
pool_offline_test="sovetnik_offline_new_aggr_pool_cv.txt.test"
sh get_pool.sh //home/geosearch/yourmary/sovetnik_toloka/toloka_pool_with_new_aggr_marks $pool_offline_test 1
./mx_ops calc -s4 matrixnet_exp.info -i "$pool_offline_test" > "$pool_offline_test.result.default"
./mx_ops calc -s4 matrixnet_irrel_cv_no_dssm.info -i "$pool_offline_test" > "$pool_offline_test.result.offline"
offline_merged="$pool_offline_test.result.merged"
# merge reaults
cat "$pool_offline_test.result.offline" | cut -f5 | paste "$pool_offline_test.result.default" - > "$offline_merged"
alias awkt='awk -v OFS="\t" -F"\t"'

printf "test exp model & offline model on offline test pool\n"
###
# run default & offline models on test part of control click pool
###
pool_control_test="sovetnik_control_pool_cv.txt.test"
sh get_pool.sh //home/geosearch/sovetnik/pool/control-2019-09-15-with-irrel-weight $pool_control_test 1 //home/geosearch/sovetnik/pool/control-2019-09-15-with-irrel-weight
./mx_ops calc -s4 matrixnet_exp.info -i "$pool_control_test" > "$pool_control_test.result.default"
./mx_ops calc -s4 matrixnet_irrel_cv_no_dssm.info -i "$pool_control_test" > "$pool_control_test.result.offline"
# merge reaults
control_merged="$pool_control_test.result.merged"
cat "$pool_control_test.result.offline" | cut -f5 | paste "$pool_control_test.result.default" - > "$control_merged"
SHOWS="$(wc -l "$control_merged" | awk '{print($1)}')"
CLICKS="$(awkt < "$control_merged" '$2 == 1' | wc -l)"
CLICKS_DEFAULT="$(awkt < "$control_merged" -v dt="$default_thres" '$2 == 1 && $5 >= dt' | wc -l)"

IRRELS=`cat "$offline_merged" | awkt -v ir="$irrel_thes" '$2 <ir {print}' | wc -l`
RELS=`cat "$offline_merged" | awkt -v ir="$irrel_thes" -v rt="$rel_thes" '$2 >= ir {print}' | wc -l`

### this is table header
echo "#|"
echo "**||"
echo "|"
for default_thres in $(seq 0.025 0.002 0.037 | sed 's/,/./')
do
    if [ $(echo "$default_thres == $current_default_thres" | bc) -eq 1 ]; then
        this="**"
    else
        this=""
    fi
    echo "${this}exp $default_thres${this} |"
done
echo "||**"

### combine thresholds of both models
for threshold in $(seq -1.8 0.3 1 | sed 's/,/./')
do
    echo "||"
    echo "offline $threshold"
    for default_thres in $(seq 0.025 0.002 0.037 | sed 's/,/./')
    do
### calc offline quality
        echo "|"
        irrels_shown=`cat "$offline_merged" | awkt -v t="$threshold" -v ir="$irrel_thes" -v dt="$default_thres" '($6 >= t && $5 >= dt) && $2 <ir {print}' | wc -l`
### calc click loss on control
        awkt < "$control_merged" \
             -v t="$threshold" -v dt="$default_thres" -v shows="$SHOWS" -v clicks="$CLICKS" -v clicks_df="$CLICKS_DEFAULT" -v irr="$irrels_shown" -v irrels="$IRRELS" \
             '($6 >= t && $5 >= dt) {s+=1; cl+=$2}END{print "loss", "shows="s/shows, "clicks="cl/clicks, "ctr="cl/s, "irrel="irr/irrels}' | tr '\t' '\n'
    done
    echo "||"
done
echo "|#"
