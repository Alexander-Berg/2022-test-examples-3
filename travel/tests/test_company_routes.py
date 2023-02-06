from travel.avia.library.python.sirena_client import SirenaClient

RESPONSE = '''<sirena>
    <answer>
        <get_company_routes company="XX">
            <departure code="MOW">
                <arrival>LED</arrival>
                <arrival>AZN</arrival>
                <arrival>AAQ</arrival>
                <arrival>SVX</arrival>
                <arrival>KRR</arrival>
                <arrival>KUF</arrival>
                <arrival>AER</arrival>
                <arrival>TJM</arrival>
            </departure>
            <departure code="UFA">
                <arrival>MOW</arrival>
            </departure>
            <departure code="AAQ">
                <arrival>MOW</arrival>
            </departure>
            <departure code="SVX">
                <arrival>MOW</arrival>
                <arrival>KRR</arrival>
                <arrival>AER</arrival>
            </departure>
            <departure code="KRR">
                <arrival>MOW</arrival>
                <arrival>SVX</arrival>
            </departure>
            <departure code="AER">
                <arrival>MOW</arrival>
                <arrival>LED</arrival>
                <arrival>MUC</arrival>
            </departure>
            <cityports>
                <city code="MOW">
                    <port>SVO</port>
                    <port>DME</port>
                    <port>VKO</port>
                </city>
                <city code="LED">
                    <port>LED</port>
                </city>
                <city code="UFA">
                    <port>UFA</port>
                </city>
                <city code="SVX">
                    <port>SVX</port>
                </city>
                <city code="KRR">
                    <port>KRR</port>
                </city>
                <city code="KUF">
                    <port>KUF</port>
                </city>
                <city code="TJM">
                    <port>TJM</port>
                </city>
                <city code="MUC">
                    <port>MUC</port>
                </city>
            </cityports>
        </get_company_routes>
    </answer>
</sirena>'''

EXCEPTED = {
    "MOW": [
        "LED",
        "AZN",
        "AAQ",
        "SVX",
        "KRR",
        "KUF",
        "AER",
        "TJM"
    ],
    "UFA": [
        "MOW"
    ],
    "AAQ": [
        "MOW"
    ],
    "SVX": [
        "MOW",
        "KRR",
        "AER"
    ],
    "KRR": [
        "MOW",
        "SVX"
    ],
    "AER": [
        "MOW",
        "LED",
        "MUC"
    ]
}


def test_company_routes():
    client = SirenaClient("-", "test", 1)
    assert EXCEPTED == client._parse_sirena_routes(RESPONSE)
