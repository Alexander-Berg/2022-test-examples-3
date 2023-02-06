DATA(
    arcadia/crypta/graph/v1/tests/UrlToGroups.yaml
    arcadia/crypta/graph/v1/tests/indevice.info
    arcadia/crypta/graph/v1/packages/rtcrypta-config/set_crypta_env.sh.template
    arcadia/metrika/uatraits/data/browser.xml
    arcadia/metrika/uatraits/data/profiles.xml
    arcadia/crypta/graph/v1/tests/testdata/fixtures/partners_flat.json
    arcadia/crypta/graph/v1/tests/testdata/fixtures/stream_metrika.json
    arcadia/crypta/graph/v1/tests/testdata/fixtures/stream_postback_extra_data.json
    arcadia/crypta/graph/v1/tests/testdata/fixtures/stream_rtblog.json
    arcadia/crypta/graph/v1/tests/testdata/fixtures/stream_soup.json
    arcadia/crypta/graph/data_import/app_metrica_month/tests/fixtures/am_log_table.spec.json
    arcadia/crypta/graph/data_import/app_metrica_month/tests/fixtures/dev_info_yt.spec.json
    arcadia/crypta/graph/data_import/app_metrica_month/tests/fixtures/fuzzy2_metrica.spec.json
    arcadia/crypta/graph/data_import/app_metrica_month/tests/fixtures/postback_extra_data.spec.json
    arcadia/crypta/graph/data_import/app_metrica_month/tests/fixtures/rtb_extra_data.spec.json
    arcadia/crypta/graph/data_import/app_metrica_month/tests/fixtures/uuid_info_yt.spec.json
    arcadia/crypta/graph/data_import/metrika_user_params/tests/fixtures/param_owners_01.json
    arcadia/crypta/graph/data_import/metrika_user_params/tests/fixtures/param_owners_01.spec.json
    arcadia/crypta/graph/data_import/metrika_user_params/tests/fixtures/mobmet_app_id_and_domains.json
    arcadia/crypta/graph/data_import/metrika_user_params/tests/fixtures/mobmet_app_id_and_domains.spec.json
    arcadia/crypta/graph/data_import/metrika_user_params/tests/fixtures/mobmet_counters_and_domains.json
    arcadia/crypta/graph/data_import/metrika_user_params/tests/fixtures/mobmet_counters_and_domains.spec.json
    arcadia/crypta/graph/data_import/webvisor/tests/fixtures/webvisor.json
    arcadia/crypta/graph/data_import/webvisor/tests/fixtures/webvisor.spec.json
    arcadia/crypta/graph/data_import/fp_parser/tests/fixtures/reqans_log.json
    arcadia/crypta/graph/data_import/fp_parser/tests/fixtures/reqans_log.spec.json
    arcadia/crypta/graph/data_import/passport/tests/fixtures/passport.json
    arcadia/crypta/graph/data_import/passport/tests/fixtures/passport.attrs.json
)

DEPENDS(
    crypta/graph/yuid_apps

    crypta/graph/v1/tests/sandbox-data

    yt/python/yt/wrapper/bin/mapreduce-yt_make
    metrika/uatraits/data

    quality/relev_tools/mx_ops
)

INCLUDE(${ARCADIA_ROOT}/crypta/lib/inc/embedded_yql.inc)
