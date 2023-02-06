def check_counts(resp, expected_total=None, expected_matched=None, expected_filter_values=None):
    assert (not resp.HasField('Error'))
    counts = resp.Counts
    if expected_total:
        assert (counts.TotalCount == expected_total)
    if expected_matched:
        assert (counts.MatchedCount == expected_matched)
    assert len(counts.AdditionalFilterCounts) == len(expected_filter_values)
    for ((expected_id, expected_count), actual) in zip(expected_filter_values, counts.AdditionalFilterCounts):
        assert expected_id == actual.UniqueId
        print(expected_count, actual.Count)
        assert expected_count == actual.Count
