#!/usr/bin/env bash
set -x -e -u -o pipefail

PREFIX=/Berkanavt
BIN_PREFIX=$PREFIX/binaries/bin
CONFIG_DIR=$PREFIX/test_data/full/data
CONFIG=$CONFIG_DIR/cuba.test.cfg
SCRIPTS=$BIN_PREFIX
COMMON_SCRIPTS=$SCRIPTS
BIN=$BIN_PREFIX/cuba

export YT_PROXY=arnold
export MR_RUNTIME=YT
export YT_PREFIX=//
export MR_TMP=home/videoindex/recommender/test_data/vitrina/tmp
export YT_SPEC='{"weight": 100}'

_scenario() {
    GLOB = localhost

    GLOB GenerateConfig:
    GLOB GenerateLearnConfig:
    GLOB DeletePrev:                    GenerateConfig
    GLOB CopyExternalData:              GenerateConfig
    GLOB PrepareSafeObjects:            CopyExternalData
    GLOB GetCryptaUserRegion:           CopyExternalData
    GLOB PrepareUids:                   CopyExternalData
    GLOB GetCryptaData:                 PrepareUids
    GLOB SubstituteUids:                PrepareUids GetCryptaUserRegion
    GLOB GetUserFilmViews:              SubstituteUids
    GLOB MergeUserFilmViews:            GetUserFilmViews
    GLOB GetTopViewedFilms:             GetUserFilmViews PrepareSafeObjects
    GLOB AssignFilmData:                GetTopViewedFilms
    GLOB GetDefaultFilms:               AssignFilmData
    GLOB CreateFilmPool:                AssignFilmData
    GLOB CalcFilmEmbeddings:            CreateFilmPool
    GLOB BuildIndexMapping:             CalcFilmEmbeddings
    GLOB CreateKnnIndex:                BuildIndexMapping

    GLOB SortRecordsWithText:           SubstituteUids
    GLOB EnrichTexts:                   SubstituteUids
    GLOB MergeDataFields:               EnrichTexts SortRecordsWithText
    GLOB JoinDataFields21:              MergeDataFields
    GLOB PrepareData4LearnPool:         MergeDataFields AssignFilmData
    GLOB CreateLearnPool:               JoinDataFields21 PrepareData4LearnPool GetCryptaData
    GLOB TrainDssm:                     CreateLearnPool

    GLOB GetFilmsOntoId:                CopyExternalData
    GLOB MakeAllFilmsPool:              GetFilmsOntoId
    GLOB CalcAllFilmEmbeddings:         MakeAllFilmsPool
    GLOB GetOntoId2Embedding:           CalcAllFilmEmbeddings

    GLOB FilterDeepViews:               MergeUserFilmViews GetOntoId2Embedding
    GLOB FilterTopNViewsInCategory:     FilterDeepViews
    GLOB AddEmbeddings2UserViews:       FilterTopNViewsInCategory
    GLOB CalcAvgEmbedding4UserViews:    AddEmbeddings2UserViews

    GLOB GetActiveUsers:                JoinDataFields21
    GLOB CreateProductionUserPool:      GetActiveUsers GetCryptaData
    GLOB CalcUserEmbeddings:            CreateProductionUserPool
    GLOB AddData2UserProfile:           CalcUserEmbeddings MergeUserFilmViews CalcAvgEmbedding4UserViews
    GLOB PrepareUserProfiles4Upload:    AddData2UserProfile
    GLOB UploadUserProfiles2Saas:       PrepareUserProfiles4Upload

    GLOB GetFilm2List:                  CopyExternalData
    GLOB GetListAvgEmbedding:           GetFilm2List CalcAllFilmEmbeddings
    GLOB BuildShard:                    CreateKnnIndex GetListAvgEmbedding
    GLOB BuildIndexAttr:                BuildShard
    GLOB UploadIndexFiles:              BuildIndexAttr

    GLOB PrepareAllFilmsEmbedding4Upload:   CalcAllFilmEmbeddings
    GLOB UploadAllFilmEmbeddings2Saas:      PrepareAllFilmsEmbedding4Upload

    GLOB SaveUserViewedFilms:               MergeUserFilmViews

    GLOB MergeVitrinaRecords4MxNet:     CopyExternalData
    GLOB ProcessVitrinaRecords4MxNet:   PrepareUids MergeVitrinaRecords4MxNet

    GLOB GetOntoId2ViewsLastMonth4MxNet:    ProcessVitrinaRecords4MxNet
    GLOB GetDate2ViewsLastMonth4MxNet:      GetOntoId2ViewsLastMonth4MxNet

    GLOB CreateDssmPool4MxNet:          JoinDataFields21 GetCryptaData ProcessVitrinaRecords4MxNet
    GLOB CalcUserEmbeddings4MxNet:      CreateDssmPool4MxNet

    GLOB PrepareUserEmbeddings4MxNet:   CalcUserEmbeddings4MxNet
    GLOB AddFilmData4MxNet:             CreateDssmPool4MxNet GetOntoId2Embedding GetDate2ViewsLastMonth4MxNet
    GLOB CalcDotProduct4MxNet:          AddFilmData4MxNet PrepareUserEmbeddings4MxNet

    GLOB Ku:                            UploadUserProfiles2Saas SaveUserViewedFilms UploadIndexFiles UploadAllFilmEmbeddings2Saas DeletePrev CalcDotProduct4MxNet TrainDssm GetDefaultFilms
}


run_mutable_table() {
    if ! `yt exists //home/videoindex/recommender/test_data/vitrina/canon/$1`; then
        shift
        $@
    fi
}


job() {
    local mode=$1
    shift
    local job=$1
    shift
    $BIN $mode -c $CONFIG -J $job $@
}

GEN_CONFIG_CMD="$SCRIPTS/gen_config --tmpl $CONFIG_DIR/cuba.test.cfg.tmpl --vars $CONFIG_DIR/vars.json --date `date --date='now' +'%Y%m%d'` --config $CONFIG"

TABLES_CHECKER="$SCRIPTS/test_check_tables -p $CONFIG_DIR/test_check.cfg --config $CONFIG"

GenerateConfig() {
    $GEN_CONFIG_CMD
}

GenerateLearnConfig() {
    echo "GenerateLearnConfig"
}

CopyExternalData() {
    echo "CopyExternalData"
}

DeletePrev() {
    echo "DeletePrev"
}

PrepareSafeObjects() {
    job prep PrepareSafeObjects
    $TABLES_CHECKER --tables SafeFilms --sorted
}

GetCryptaUserRegion() {
    job prep GetCryptaUserRegion
    # table: UserRegionRaw will be expanded in task SubstituteUids
}

PrepareUids() {
    job prep PrepareUids
    $TABLES_CHECKER --tables Id2Puid Uid2Id Id2Uid --sorted
}

GetCryptaData() {
    job prep GetCryptaData
    $TABLES_CHECKER --tables CryptaData --sorted
}

SubstituteUids() {
    job prep SubstituteUids

    local tables=(%ERecsWithText %ERecsWithoutText %EUrlText %EDataFields1)
    $TABLES_CHECKER --tables UserRegionRaw ${tables[@]//%//data/records/}
}

GetUserFilmViews() {
    job prep GetUserFilmViews
    $TABLES_CHECKER --tables UserFilmViewsWide UserFilmViewsFresh /data/user/film_views.raw --sorted
}

GetTopViewedFilms() {
    job prep GetTopViewedFilms
    $TABLES_CHECKER --tables FreshFilmsFreq /data/films/fresh_freq.low_count /data/films/fresh_freq.declined --sorted
}

AssignFilmData() {
    job prep AssignFilmData
    $TABLES_CHECKER --tables TopFilmsData TopFilmsData4Pool TopFilmsData4Pool.error UnknownTopFilms.error --sorted
}

GetDefaultFilms() {
    run_mutable_table data/films/defaultfilter_basic job prep GetDefaultFilms

    local filters=(%none %basic %family %tv_app %vh %tv_app_kids %ott)
    local table=/data/films/default
    $TABLES_CHECKER --tables DefaultFilms ${filters[@]//%/${table}filter_} --sorted --mutable
    $TABLES_CHECKER --tables ${table}err --mutable
}

CreateFilmPool() {
    job prep CreateFilmPool
    $TABLES_CHECKER --tables ProdFilmPool
}

SortRecordsWithText() {
    job prep SortRecordsWithText
    # canon tables is not sorted (task: SubstituteUids)
}

EnrichTexts() {
    # tables ERecsTextEnriched and EDataFields2 recreate

    job prep EnrichTexts

    local prefix=//home/videoindex/recommender/test_data/vitrina
    local tabs=(data/records/EDataFields2 data/records/ERecsTextEnriched)

    for t in ${tabs[@]}; do
        if `yt exists $prefix/canon/$t`; then
            yt copy -f $prefix/canon/$t $prefix/cuba/$t
        else
            $TABLES_CHECKER --tables /$t --sorted --mutable
        fi
    done
}

MergeDataFields() {
    job prep MergeDataFields
    $TABLES_CHECKER --tables DataFieldsMerged --sorted
}

JoinDataFields21() {
    local table_name=DataFields21

    run_mutable_table $table_name job prep JoinDataFields21

    $TABLES_CHECKER --tables $table_name --sorted --mutable
}

PrepareData4LearnPool() {
    job prep PrepareData4LearnPool
    $TABLES_CHECKER --tables PoolFilteredRecords --sorted
}

CreateLearnPool() {
    job prep CreateLearnPool
    $TABLES_CHECKER --tables PoolTrain PoolTest --sorted
}

GetActiveUsers() {
    local table_name=ActiveUsers

    run_mutable_table $table_name job prep GetActiveUsers

    $TABLES_CHECKER --tables ActiveUsersData $table_name --sorted --mutable
}

CreateProductionUserPool() {
    job prep CreateProductionUserPool
    $TABLES_CHECKER --tables ProdUserPool --sorted
}

CalcFilmEmbeddings() {
    local table_name=prod/embeddings/vitrina/Vitrina50/doc

    run_mutable_table $table_name job prep CalcFilmEmbeddings

    $TABLES_CHECKER --tables /$table_name --sorted --mutable
}

BuildIndexMapping() {
    job prep BuildIndexMapping
    $TABLES_CHECKER --tables /index/mapping --sorted
}

CreateKnnIndex() {
    job prep CreateKnnIndex

    local tables=(\
        %/embedding/PN_Vitrina#OT_OntoId_PN_Vitrina#ET_OntoId_0_RT_Sum\
        %/profile_data/PN_Vitrina#OT_OntoId\
        %/profile_data/PN_Vitrina#OT_OntoId.inv_id_mapping\
        %/HNSW_PN_Vitrina#OT_OntoId_PN_Vitrina#ET_OntoId_0_RT_Sum\
    )
    $TABLES_CHECKER --tables ${tables[@]//%//index} --sorted
}

BuildShard() {
    job prep BuildShard

    local tables=(\
        %/archive.idx\
        %/categ_top_docs.idx\
        %/film.factors\
        %/filter_by_categ.idx\
        %/filter_by_categ.idx.doc2categ\
        %/filter_by_country.idx\
        %/filter_by_filter.idx\
    )
    $TABLES_CHECKER --tables IndexMapping ${tables[@]//%//index} --sorted
}

BuildIndexAttr() {
    $SCRIPTS/gen_indexattr -c $CONFIG

    local shard_dir=$PREFIX/vitrina_index/database
    local arr=(`ls --ignore="*.gold" $shard_dir`)
    for f in ${arr[@]}; do
        local file_path=//home/videoindex/recommender/test_data/vitrina/canon/shard_dir/$f
        local doc=$shard_dir/$f
        if `yt exists $file_path`; then
            yt download $file_path > $doc.gold
            diff -a $doc $doc.gold
        else
            cat $doc | yt upload $file_path
        fi
    done
}

UploadIndexFiles() {
    echo "UploadIndexFiles"
}

CalcUserEmbeddings() {
    local table_name=prod/embeddings/vitrina/Vitrina50/user

    run_mutable_table $table_name job prep CalcUserEmbeddings

    $TABLES_CHECKER --tables /$table_name --sorted --mutable
}

AddData2UserProfile() {
    job prep AddData2UserProfile
    $TABLES_CHECKER --tables UserProfiles --sorted
}

PrepareUserProfiles4Upload() {
    job prep PrepareUserProfiles4Upload
    $TABLES_CHECKER --tables UserProfiles4Upload /backup/20180701/profiles/user_profiles
}

UploadUserProfiles2Saas() {
    echo "UploadUserProfiles2Saas"
}

TrainDssm() {
    echo "TrainDssm"
}

GetFilm2List() {
    job prep GetFilm2List
    $TABLES_CHECKER --tables Film2List --sorted
}

GetListAvgEmbedding() {
    local table_name=prod/embeddings/vitrina/Vitrina50/list

    run_mutable_table $table_name job prep GetListAvgEmbedding

    $TABLES_CHECKER --tables ListFilmEmbedding /$table_name --sorted --mutable
}

GetFilmsOntoId() {
    job prep GetFilmsOntoId
    $TABLES_CHECKER --tables AllFilmOntoId --sorted
}

MakeAllFilmsPool() {
    job prep MakeAllFilmsPool
    $TABLES_CHECKER --tables AllFilmDataPool StubDataErr StubObjErr
}

CalcAllFilmEmbeddings() {
    local table_name=prod/embeddings/film/Vitrina50/doc

    run_mutable_table $table_name job prep CalcAllFilmEmbeddings

    $TABLES_CHECKER --tables /$table_name --sorted --mutable
}

GetOntoId2Embedding() {
    job prep GetOntoId2Embedding
    $TABLES_CHECKER --tables OntoId2Embedding --sorted
}

PrepareAllFilmsEmbedding4Upload() {
    job prep PrepareAllFilmsEmbedding4Upload
    $TABLES_CHECKER --tables AllFilmEmbeddings4Saas /backup/20180701/profiles/all_film_embeddings
}

UploadAllFilmEmbeddings2Saas() {
    echo "UploadAllFilmEmbeddings2Saas"
}

MergeUserFilmViews() {
    local table_name=UserFilmViewesAllWide

    run_mutable_table $table_name job prep MergeUserFilmViews

    $TABLES_CHECKER --tables $table_name --sorted --mutable
}

SaveUserViewedFilms() {
    job prep SaveUserViewedFilms
    $TABLES_CHECKER --tables DataVitrinaUserFilmViews --sorted
}

FilterDeepViews() {
    job prep FilterDeepViews
    $TABLES_CHECKER --tables UserFilmDeepViews --sorted
}

FilterTopNViewsInCategory() {
    job prep FilterTopNViewsInCategory
    $TABLES_CHECKER --tables UserFilmDeepViewsTopN --sorted
}

AddEmbeddings2UserViews() {
    job prep AddEmbeddings2UserViews
    $TABLES_CHECKER --tables UserViewEmbeddings --sorted
}

CalcAvgEmbedding4UserViews() {
    local table_name=UserViewEmbeddingsAvg

    run_mutable_table $table_name job prep CalcAvgEmbedding4UserViews

    $TABLES_CHECKER --tables $table_name --sorted --mutable
}

Ku() {
    echo "Ku!"
}

MergeVitrinaRecords4MxNet() {
    job prep MergeVitrinaRecords4MxNet
    $TABLES_CHECKER --tables MxNetRecords
}

ProcessVitrinaRecords4MxNet() {
    local table_name=MxNetRecordsProcessed

    run_mutable_table $table_name job prep ProcessVitrinaRecords4MxNet

    $TABLES_CHECKER --tables $table_name --mutable
}

GetOntoId2ViewsLastMonth4MxNet() {
    job prep GetOntoId2ViewsLastMonth4MxNet
    $TABLES_CHECKER --tables MxNetOntoId2Views --sorted
}

GetDate2ViewsLastMonth4MxNet() {
    job prep GetDate2ViewsLastMonth4MxNet
    $TABLES_CHECKER --tables MxNetDate2Views
}

CreateDssmPool4MxNet() {
    job prep CreateDssmPool4MxNet
    $TABLES_CHECKER --tables MxNetUserPool /learn/mxnet/user_pool.test --sorted
}

CalcUserEmbeddings4MxNet() {
    local table_name=MxNetUserEmbeddingsRaw

    run_mutable_table $table_name job prep CalcUserEmbeddings4MxNet

    $TABLES_CHECKER --tables $table_name --mutable
}

PrepareUserEmbeddings4MxNet() {
    job prep PrepareUserEmbeddings4MxNet
    $TABLES_CHECKER --tables MxNetUserEmbeddings --sorted
}

AddFilmData4MxNet() {
    job prep AddFilmData4MxNet
    $TABLES_CHECKER --tables MxNetRecordsWithFilmData
}

CalcDotProduct4MxNet() {
    job prep CalcDotProduct4MxNet
    $TABLES_CHECKER --tables MxNetPool
}

"$@"
