from market.dynamic_pricing.parsing.tools.exception_list.exceptions import normalize_url


def test_urls():
    assert normalize_url('https://www.wildberries.ru/catalog/13822736/detail.aspx') == 'https://www.wildberries.ru/catalog/13822736/detail.aspx'
    assert normalize_url('http://www.wildberries.ru/catalog/13822736/detail.aspx') == 'https://www.wildberries.ru/catalog/13822736/detail.aspx'
    assert normalize_url('https://wildberries.ru/catalog/13822736/detail.aspx') == 'https://www.wildberries.ru/catalog/13822736/detail.aspx'

    assert normalize_url('https://www.ozon.ru/context/detail/id/147424669/') == 'https://www.ozon.ru/context/detail/id/147424669/'
    assert normalize_url('https://www.ozon.ru/context/detail/id/147424669') == 'https://www.ozon.ru/context/detail/id/147424669/'
    assert normalize_url('http://ozon.ru/context/detail/id/147424669') == 'https://www.ozon.ru/context/detail/id/147424669/'
    assert normalize_url('https://www.ozon.ru/context/detail/id/140490842/?asb=VXxKvxkGYkrRujCtnLyMdktEAGlLs2hMTBq4t9r%252BJA8%253D') == 'https://www.ozon.ru/context/detail/id/140490842/'

    assert (normalize_url(
        'https://spb.vseinstrumenti.ru/rashodnie-materialy/dlya-sil-teh/dlya-svarochnyh-rabot/prochie-aksessuary/gazovye-ballony/propanovye/spets/12-l-bytovoj-s-ventilem-vb-2-nzga-sv-bal12n/'
        )
        ==
        'https://spb.vseinstrumenti.ru/rashodnie-materialy/dlya-sil-teh/dlya-svarochnyh-rabot/prochie-aksessuary/gazovye-ballony/propanovye/spets/12-l-bytovoj-s-ventilem-vb-2-nzga-sv-bal12n/'
    )
