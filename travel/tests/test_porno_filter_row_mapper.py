from travel.hotels.suggest.builder.dictionary_builder import PornoFilterRowMapper


class TestPornoFilterRowMapper:
    MAPPER = PornoFilterRowMapper()

    def test_well_known_porno_words(self):
        assert self.MAPPER.is_porno('Фраза содержащая слово 24video')
        assert self.MAPPER.is_porno('Фраза содержащая слово БДСМ')
        assert not self.MAPPER.is_porno('Фраза без каких-либо нехороших слов')
        assert self.MAPPER.is_porno('Непонятный кейс пу#ин, для которого пришлось расширить regular expression')
