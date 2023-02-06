#!/usr/bin/env bash

ARCADIA="/home/kruglikovn/arc/arcadia"
DATA="/extra_hdd/data/stuff/plugin/user_hour"
export PYTHONPATH="$ARCADIA/quality/yaqlib"

SUFFIX_A="_a.html"
SUFFIX_B="_b.html"

QUERY="кот царапается и кусается что делать"
NAME="cat"
$ARCADIA/quality/sbs/sbs-apply-one-plugin.py \
    -q "$QUERY" \
    -i "$DATA/$NAME$SUFFIX_A" \
    -o "$DATA/$NAME$SUFFIX_B" \
    --plugin-file="$ARCADIA/search/alice/snippets/lib/sbs/organic_neurosnippets.py" \
    --system-type="yandex-web-desktop"

QUERY="узнать где посылка по трек номеру"
NAME="track"
$ARCADIA/quality/sbs/sbs-apply-one-plugin.py \
    -q "$QUERY" \
    -i "$DATA/$NAME$SUFFIX_A" \
    -o "$DATA/$NAME$SUFFIX_B" \
    --plugin-file="$ARCADIA/search/alice/snippets/lib/sbs/organic_neurosnippets.py" \
    --system-type="yandex-web-desktop"

QUERY="что посмотреть во франции"
NAME="france"
$ARCADIA/quality/sbs/sbs-apply-one-plugin.py \
    -q "$QUERY" \
    -i "$DATA/$NAME$SUFFIX_A" \
    -o "$DATA/$NAME$SUFFIX_B" \
    --plugin-file="$ARCADIA/search/alice/snippets/lib/sbs/organic_neurosnippets.py" \
    --system-type="yandex-web-desktop"

QUERY="почему трава зелёная"
NAME="grass"
$ARCADIA/quality/sbs/sbs-apply-one-plugin.py \
    -q "$QUERY" \
    -i "$DATA/$NAME$SUFFIX_A" \
    -o "$DATA/$NAME$SUFFIX_B" \
    --plugin-file="$ARCADIA/search/alice/snippets/lib/sbs/organic_neurosnippets.py" \
    --system-type="yandex-web-desktop"

QUERY="сколько весит кит"
NAME="kit"
$ARCADIA/quality/sbs/sbs-apply-one-plugin.py \
    -q "$QUERY" \
    -i "$DATA/$NAME$SUFFIX_A" \
    -o "$DATA/$NAME$SUFFIX_B" \
    --plugin-file="$ARCADIA/search/alice/snippets/lib/sbs/organic_neurosnippets.py" \
    --system-type="yandex-web-desktop"
