#!/usr/bin/env bash

# Put your token to ~/.vhrc:
# --oauth-token=YOUR_TOKEN
# See https://a.yandex-team.ru/arc/trunk/arcadia/nirvana/valhalla/docs/reference/run_kwargs.md#oauth_token

# Usage:
# ./generate_test_graph.sh
#     [-w "04ceef6c-8a72-4c12-8b6b-dc93a7d1046e"]  workflow guid
#     [-i "1f1a902c-d7f3-44b9-bb72-bccf22d688ab"]  input block id
#     [--yt-token "meow_yt_token"]                 yt token (nirvana secret)
#     [--yql-token "meow_yql_token"]               yql token (nirvana secret)
#     [-t "2020-07-10T16:00:00+0300"]              custom timestamp for caching
#     [--no-timestamp]                             do not set timestamp
#     [-v]                                         validate graph
#     [-r]                                         run graph

set -x

./lost_preview_status -c test_config.json "$@"
