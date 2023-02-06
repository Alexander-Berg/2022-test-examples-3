from search.wizard.entitysearch.tools.es_hook_notifier.lib.consts import FRESH_RESOURCE, REALTIME_RESOURCE_TAR
from search.wizard.entitysearch.tools.es_hook_notifier.lib.parser import (
    parse_args,
    parse_updates,
    MODIFIED,
    REMOVED,
    ADDED,
)

UNKNOWN_RESOURCE = "unknown_resource.exe"


def test_input_update_types():
    args = parse_args(["+%s" % FRESH_RESOURCE, "!%s" % REALTIME_RESOURCE_TAR, "-%s" % UNKNOWN_RESOURCE])
    result = parse_updates(args.resources)
    assert result == {ADDED: [FRESH_RESOURCE], MODIFIED: [REALTIME_RESOURCE_TAR], REMOVED: [UNKNOWN_RESOURCE]}
