import rm_fml


def test_rm_regex():
    fml_name = 'xxx'
    orig = [
        'some text',
        '',
        '',
        'FROM_SANDBOX(777 OUT some_file)',  # do not match, bad extension
        'FROM_SANDBOX(777 OUT some_file.ext)',  # do not match, bad extension
        'FROM_SANDBOX(777 OUT some_fml.info)',  # do not match with fml_name
        'FROM_SANDBOX(777 OUT xxx.xtd)  # match comment',  # match with comment
        'FROM_SANDBOX(777 OUT xxx.xtd) ',  # match trailing space
        'FROM_SANDBOX(777 OUT xxx.xtd)',  # best case
        'FROM_SANDBOX(777 OUT xxx.xtd)',  # best case
        ' FROM_SANDBOX(777 OUT xxx.xtd)',  # best case
        'FROM_SANDBOX(777 OUT xxx.xtd) do not match it',  # do not match in case of some trash in line
        'FROM_SANDBOX(asd OUT xxx.xtd)',  # do not match in case of bad resource id
    ]
    etalon = [
        'some text',
        '',
        '',
        'FROM_SANDBOX(777 OUT some_file)',
        'FROM_SANDBOX(777 OUT some_file.ext)',
        'FROM_SANDBOX(777 OUT some_fml.info)',
        'FROM_SANDBOX(777 OUT xxx.xtd) do not match it',
        'FROM_SANDBOX(asd OUT xxx.xtd)',
    ]
    orig_text = '\n'.join(orig)
    etalon_text = '\n'.join(etalon)
    res_text = rm_fml.rm_fml_from_make_file_text(orig_text, fml_name)
    assert etalon_text == res_text
