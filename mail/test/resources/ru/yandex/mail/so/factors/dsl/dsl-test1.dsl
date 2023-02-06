create concat extractor concat_2(string a, string b) -> string {
    delimiter = ,\\ 
}

create concat extractor bare_concat_3(string a, string b, string c) -> string {
}

create chain extractor my_chain(string prefix, string suffix, string h, string w) -> string, string, string {
    concat_2(h, w) -> hw;
    concat_2(w, h) -> wh;
    bare_concat_3 as direct_concat(prefix, hw, suffix) -> direct;
    bare_concat_3 as reverse_concat(prefix, wh, suffix) -> reverse;
    return direct;
    return reverse if not is_any_null(h);
    return w if is_any_null(h) else h;
}

create chain extractor string_x9(string str) -> string {
    bare_concat_3(str, str, str) -> str3;
    bare_concat_3(str3, str3, str3) -> str9;
    return str9;
}

create chain extractor string_x27(string str) -> string {
    string_x9(str) -> str9;
    bare_concat_3(str9, str9, str9) -> str27;
    return str27;
}

create chain extractor borders_x3(string a, string b, string c) -> string, string {
    async bare_concat_3 as make_a3(a, a, a) -> a3;
    async bare_concat_3 as make_c3(c, c, c) -> c3;
    bare_concat_3 as make_all(a3, b, c3) -> all;
    concat_2 as make_borders(a3, c3) -> borders;
    return all;
    return borders;
}

create chain extractor sequential_concat(string a, string b, string c) -> string {
    create concat extractor colon_concat(string a, string b) -> string {
        # test single char delimiter
        delimiter = :
    }

    # now apply concat twice
    colon_concat(a, b) -> tmp;
    colon_concat(tmp, c) -> result;
    return result;
}

create chain extractor bare_concat_2(string a, string b) -> string {
    trace bare_concat_3(a, b, null) -> c;
    return c;
}

create chain extractor multi_returns(string a, string b, string c) -> string, string, string, string, string, string, string, string, string, string {
    return null, a, c;
    return a, null, b, c if not is_any_null(c);
    return a, b, null if is_any_null(c) else a, null, c;
}

create chain extractor const_concat_1(string a) -> string, string, string {
    concat_2("\"Preamble\":\nHello", "world") -> str1;
    bare_concat_3(str1, " -> ", a) -> str2;
    return str2 if not is_any_null("test") else "nothing";
    return "suffix", "puffix";
}

create chain extractor const_concat_2(string a) -> string, string, string {
    concat_2("Hello", "world") -> str1;
    bare_concat_3(str1, " -> ", a) -> str2;
    return str2 if not is_any_null(null) else "nothing";
    return "suffix", "puffix";
}

create chain extractor critical_concat(string a, string b) -> string, string, string {
    concat_2(a, a) -> a2;
    critical assert_not_null(b) -> not_null_b;
    concat_2(a, not_null_b) -> result;
    return a2;
    return result;
    return "suffix";
}

create chain extractor wrap_critical_concat(string a, string b) -> string, string, string, string, string {
    critical_concat(a, b) -> r1, r2, r3;
    return "before", r1, r2, r3, "after";
}

create chain extractor bypass_critical_concat(string a, string b) -> string, string, string, string, string {
    critical critical_concat(a, b) -> r1, r2, r3;
    return "before", r1, r2, r3, "after";
}

