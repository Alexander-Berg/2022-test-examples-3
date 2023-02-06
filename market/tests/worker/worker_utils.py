import enum

AMORE_AUTOSTRATEGY_SIZE = 4
AMORE_AUTOSTRATEGY_BUNDLE_SIZE = 12


class StrategyType(enum.IntEnum):
    Disabled = 1
    Positional = 2
    Drr = 3
    Cpo = 4
    Cpa = 5


class TDisabled:
    as_type = StrategyType.Disabled

    def __str__(self):
        return 'Disabled'


class TPositional:
    as_type = StrategyType.Positional

    def __init__(self, pos=None, max_bid=None):
        self.Position = pos
        self.MaxBid = max_bid

    def __str__(self):
        return 'Pos={}, MaxBid={}'.format(self.Position, self.MaxBid)


class TDrr:
    as_type = StrategyType.Drr

    def __init__(self, drr=None):
        self.Drr = drr

    def __str__(self):
        return 'Drr={}'.format(self.Drr)


class TCpo:
    as_type = StrategyType.Cpo

    def __init__(self, cpo=None):
        self.Cpo = cpo

    def __str__(self):
        return 'Cpo={}'.format(self.Cpo)


class TCpa:
    as_type = StrategyType.Cpa

    def __init__(self, cpa=None):
        self.Cpa = cpa

    def __str__(self):
        return 'Cpa={}'.format(self.Cpa)


class TStrategyBundle:
    def __init__(self, as_id=None, production=None, experimental=None):
        self.Id = as_id
        self.Production = production
        self.Experimental = experimental

    def __str__(self):
        return "Budnle Id={}, Production {}, Experimental {}".\
            format(self.Id, self.Production, self.Experimental)


class TStrategyWithDatasourceId:
    def __init__(self, as_id=None, production=None, datasourceId=None):
        self.Id = as_id
        self.Production = production
        self.DatasourceId = datasourceId

    def __str__(self):
        return "Budnle Id={}, Production {}, DatasourceId {}".\
            format(self.Id, self.Production, self.DatasourceId)


def parse_autostrategy(data: bytes):
    assert len(data) == AMORE_AUTOSTRATEGY_SIZE
    as_type = int.from_bytes(bytes=data[0:1], byteorder='little')
    if as_type == StrategyType.Disabled:
        return TDisabled()
    elif as_type == StrategyType.Positional:
        pos = int.from_bytes(bytes=data[1:2], byteorder='little')
        max_bid = int.from_bytes(bytes=data[2:4], byteorder='little')
        return TPositional(pos, max_bid)
    elif as_type == StrategyType.Drr:
        drr = int.from_bytes(bytes=data[1:3], byteorder='little')
        return TDrr(drr)
    elif as_type == StrategyType.Cpo:
        cpo = int.from_bytes(bytes=data[1:3], byteorder='little')
        return TCpo(cpo)
    elif as_type == StrategyType.Cpa:
        cpa = int.from_bytes(bytes=data[1:3], byteorder='little')
        return TCpa(cpa)
    else:
        raise RuntimeError('Unknown autostrategy type: {}'.format(as_type))


def parse_amore_data(data: bytes):
    assert len(data) == AMORE_AUTOSTRATEGY_BUNDLE_SIZE
    as_id = int.from_bytes(bytes=data[0:4], byteorder='little')
    bundle = TStrategyBundle(as_id=as_id)
    bundle.Production = parse_autostrategy(data[4:8])
    bundle.Experimental = parse_autostrategy(data[8:12])
    return bundle


def parse_amore_data_with_datasource_id(data: bytes):
    assert len(data) == AMORE_AUTOSTRATEGY_BUNDLE_SIZE
    as_id = int.from_bytes(bytes=data[0:4], byteorder='little')
    bundle = TStrategyBundle(as_id=as_id)
    bundle.Production = parse_autostrategy(data[4:8])
    bundle.DatasourceId = int.from_bytes(bytes=data[8:12], byteorder='little')
    return bundle
