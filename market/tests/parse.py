import json
import pytest

from library.python.resource import resfs_read
from market.dynamic_pricing_parsing.regional_competera.bin.parse import parse_file


@pytest.mark.asyncio
async def test_parse_file():
    input = resfs_read('market/dynamic_pricing_parsing/regional_competera/tests/competera_response.json')
    input = json.loads(input)

    output_corr = resfs_read('market/dynamic_pricing_parsing/regional_competera/tests/competera_response_parsed.json')
    output_corr = json.loads(output_corr)

    output = await parse_file(input)
    assert output == output_corr
