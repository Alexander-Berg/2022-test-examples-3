INCLUDE(${ARCADIA_ROOT}/mail/library/html/sanitizer/sanitizer2_config/configs.inc)
INCLUDE(${ARCADIA_ROOT}/mail/so/daemons/cretur/cretur/test_base/recipe.inc)
INCLUDE(${ARCADIA_ROOT}/mail/so/libs/java/received_parser/test/test-data.inc)
INCLUDE(${ARCADIA_ROOT}/mail/so/libs/unperson/ut/test-data.inc)

DATA(
    arcadia/mail/so/daemons/so2/so2_config/files
    arcadia/mail/library/tikaite/test/resources
    arcadia/mail/tools/nanny_helpers/nanny_service_base/files
    arcadia/mail/so/libs/java/so_factors/test/resources/ru/yandex/mail/so/factors/hnsw/spam-samples.json
    # DssmAllCleanWithAttachments
    sbr://1605947802
    # dssm for extract embeds for SODEV-2130
    sbr://1683074274
    # taiga-epoch10.bin
    sbr://2090177243
)

ENV(MKL_DOMAIN_NUM_THREADS="MKL_DOMAIN_ALL=1; MKL_DOMAIN_BLAS=1; MKL_DOMAIN_FFT=1")
ENV(MKL_DYNAMIC=FALSE)
ENV(MKL_NUM_THREADS=1)
ENV(OMP_DYNAMIC=FALSE)
ENV(OMP_NUM_THREADS=1)
ENV(OMP_SCHEDULE="static,1")

