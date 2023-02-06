#!/usr/bin/env bash
#python ./rtcc.py control element --input-file=data/search/element/sources_list/noapache.dat --command=PRINT --p=IMGS --eltype=sources_list --el=PICASSOC_TEST | python ./rtcc.py control element --input-file=- --command=FILTER --eltype=sources_list --el=PICASSOC_TEST

for cluster in "PRIEMKA-IN-PRODUCTION" "PRODUCTION" "HAMSTER" "RC" "NOAPACHE" "REARR" "STABLE" "REPORT" "TEMPLATES"
do
    for location in "MAN" "SAS" "MSK"
    do
        for region in "COM" "TUR" "RKUB"
        do
            DATA=$(""python ./rtcc.py control element --input-file=data/search/element/sources_list/noapache.dat --command=PRINT --p=IMGS --c=$region --l=$location --s=$cluster --eltype=sources_list --el=PICASSOC_TEST | python ./rtcc.py control element --input-file=- --command=FILTER --eltype=sources_list --el=PICASSOC_TEST | awk -F "\t" '{print $2}'"")
            if [ ! -z "$DATA" ]
            then
                NEW_DATA=${DATA/"I@a_prj_imgs-rq2 . I@a_itype_mmeta . I@a_geo_man"/"C@HEAD . I@MAN_IMGS_RQ2_BASE_MMETA"}
                NEW_DATA=${NEW_DATA/"I@a_prj_imgs-rq2 . I@a_itype_mmeta . I@a_geo_sas"/"C@HEAD . I@SAS_IMGS_RQ2_BASE_MMETA"}
                NEW_DATA=${NEW_DATA/"I@a_prj_imgs-rq2 . I@a_itype_mmeta . I@a_geo_msk"/"C@HEAD . I@MSK_FOL_IMGS_RQ2_BASE_MMETA"}
                python ./rtcc.py control element --input-file=data/search/element/sources_list/noapache.dat --p=IMGS --c=$region --l=$location --s=$cluster --command=REPLACE --eltype=sources_list --el="PICASSOC_TEST" --data="$NEW_DATA" >> data/search/element/sources_list/noapache.dat.new
                mv data/search/element/sources_list/noapache.dat.new data/search/element/sources_list/noapache.dat
            fi
        done
    done
done
