# *- encoding: utf-8 -*
def test_all_exp_files_equal_to_prod(suggest_storage):
    """
    Для эксперимента с новой табличкой популярности нужно иметь копию продовой выгрузки в качестве
    экспериментальной выгрузки
    """
    remove_exp_from_type = lambda s: s[:-len('_exp.xml')] + '.xml'
    for exp_suggest_type in suggest_storage.all_exp_suggests_types:
        suggest_type = remove_exp_from_type(exp_suggest_type)

        assert len(suggest_storage[exp_suggest_type]) == len(suggest_storage[suggest_type])

        all_exp = suggest_storage.select_from(exp_suggest_type)
        all_prod = suggest_storage.select_from(suggest_type)

        for i in range(len(all_exp)):
            assert all_exp[i] == all_prod[i]
