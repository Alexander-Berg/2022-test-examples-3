# *- encoding: utf-8 -*
import os


def test_all_suggests_types_has_no_exp_files(suggest_storage):
    """
    Файл all_suggests_types.txt должен содержать только белые не экспериментальные файлы,
    это такие файлы, которые заканчиваются на _exp.xml. Данный файл может сожержать
    файлы использованные в старом формате экспериментов, они оканчиваются на _expN.txt, где
    N -- любое целое неотрицательное число.
    """
    gendir = suggest_storage.get_gendir()
    with open(os.path.join(gendir, 'all_suggests_types.txt'), 'r') as f:
        all_suggests_types = [suggest_type.strip() for suggest_type in f]

    for suggest_type in all_suggests_types:
        assert not suggest_type.endswith('_exp.xml'), '%s founded in all_suggests_types.txt' % suggest_type


def test_all_exp_suggests_types_has_only_exp_files(suggest_storage):
    """
    Файл all_exp_suggests_types.txt должен содержать только белые экспериментальные файлы,
    это такие файлы, которые заканчиваются на _exp.xml. Любые другие файлы не должны быть перечислены в этом
    файле.
    """
    gendir = suggest_storage.get_gendir()
    with open(os.path.join(gendir, 'all_exp_suggests_types.txt'), 'r') as f:
        all_exp_suggests_types = [suggest_type.strip() for suggest_type in f]

    for suggest_type in all_exp_suggests_types:
        assert not suggest_type.endswith('_exp.xml'), '%s founded in all_exp_suggests_types.txt' % suggest_type
