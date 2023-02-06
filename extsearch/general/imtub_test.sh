# --------------------------------------------------------------------------- #
# Make imtub base
# --------------------------------------------------------------------------- #

index-dump-thumbids() {
    index-init-common
    index-init-master

    call-ytbin idxthumb DumpIndex --shard-count ${INPUTDOC_SHARD_COUNT}
}

index-diff-thumbids() {
    true
}

index-check-thdbportion() {
    true
}

index-check-filtering-success() {
    true
}

index-filter-big-thumbs() {
    true
}

index-consume-taas-big-thumbs() {
    true
}

index-imtubs-finish() {
    true
}

index-garbage-dump-thumbids() {
    true
}
