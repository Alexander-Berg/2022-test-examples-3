pragma critical;

create concat extractor concat_2(string a, string b) -> string {
    delimiter = ,\\ 
}

create chain extractor critical_concat(string a, string b) -> string {
    assert_not_null(b) -> not_null_b;
    concat_2(a, not_null_b) -> result;
    return result;
}

create chain extractor noncritical_concat(string a, string b) -> string {
    noncritical critical_concat(a, b) -> result;
    return result;
}

