import json


class Directory:
    @property
    def path(self):
        return self._path

    def __init__(self, path):
        self._path = path

    def create(self, yt_client):
        yt_client.create("map_node", self.path, recursive=True, ignore_existing=True)

    def remove(self, yt_client):
        yt_client.remove(self.path, recursive=True, force=True)


class TableBase:
    """
    1. Table knows where it resides
    2. Table may create/remove itself
    """

    schema = None

    @property
    def path(self):
        return self._path

    def __init__(self, path):
        self._path = path
        self._rows = []

    def clear(self):
        self._rows = []

    def add_row(self):
        return self

    def create(self, yt_client):
        attributes = {"schema": self.schema} if self.schema else None
        yt_client.create("table", self.path, recursive=True, attributes=attributes)
        yt_client.write_table(self.path, self._rows)

    def remove(self, yt_client):
        yt_client.remove(self.path, recursive=True, force=True)


class ConfigTable(TableBase):
    def add_row(self, group_id,
                margin, margin_adj_alg, margin_adj_config,
                pricing_alg, pricing_config,
                checker_alg, checker_config,
                is_autostrategy=True):
        self._rows.append({
            "group_id": int(group_id),
            "is_autostrategy": bool(is_autostrategy),
            "margin": float(margin),
            "margin_adj_alg": margin_adj_alg,
            "margin_adj_config": json.dumps(margin_adj_config),
            "pricing_alg": pricing_alg,
            "pricing_config": json.dumps(pricing_config),
            "checker_alg": checker_alg,
            "checker_config": json.dumps(checker_config),
        })
        return self


class PriceGroup:
    def __init__(self, name, priority, base_rule_name):
        self.name = name
        self.priority = priority
        self.base_rule_name = base_rule_name


class PriceGroups:
    deadstock = PriceGroup('Автораспродажа', 1, '')
    rrc = PriceGroup('РРЦ', 2, '')
    fix = PriceGroup('ФиксЦен', 3, '')
    dco = PriceGroup('ДЦО', 4, 'autostrategy')


class ErpInputTable(TableBase):
    schema = [
        {"name": "group_id", "type": "int64", "sort_order": "ascending"},
        {"name": "market_sku", "type": "int64"},
        {"name": "shop_sku", "type": "string"},
        {"name": "hid", "type": "int64"},
        {"name": "upper_bound", "type": "double"},
        {"name": "current_price", "type": "double"},
        {"name": "lower_bound", "type": "double"},
        {"name": "purchase_price", "type": "double"},
        {"name": "stock", "type": "int64"},
        {"name": "blue_min_3p_price", "type": "double"},
        {"name": "max_available_price", "type": "double"},
        {"name": "ref_min_price", "type": "double"},
        {"name": "ref_min_soft_price", "type": "double"},
        {"name": "white_min_price", "type": "double"},
        {"name": "white_median_rpice", "type": "double"},
        {"name": "is_promo", "type": "boolean"},
        {"name": "is_kvi", "type": "boolean"},
        {"name": "cost", "type": "double"},
        {"name": "price_group_type", "type": "string"},
        {"name": "price_group_priority", "type": "int64"},
        {"name": "base_rule_name", "type": "string"}
    ]

    def add_row(self, group_id, msku, ssku, high_price, current_price, low_price, purchase_price,
                hid=1, stock=1000000, blue_min_3p_price=None, max_available_price=None, white_min_price=None,
                white_median_price=None, ref_min_price=None, ref_min_soft_price=None,
                is_promo=False, is_kvi=False, cost=None, price_group=PriceGroups.dco):
        self._rows.append({
            "group_id": int(group_id),
            "market_sku": int(msku),
            "shop_sku": ssku,
            "upper_bound": None if high_price is None else float(high_price),
            "current_price": float(current_price),
            "lower_bound": None if low_price is None else float(low_price),
            "purchase_price": float(purchase_price),
            "stock": int(stock),
            "blue_min_3p_price": None if blue_min_3p_price is None else float(blue_min_3p_price),
            "max_available_price": None if max_available_price is None else float(max_available_price),
            "white_min_price": None if white_min_price is None else float(white_min_price),
            "is_promo": is_promo,
            "is_kvi": is_kvi,
            "cost": None if cost is None else float(cost),
            "price_group_type": price_group.name,
            "price_group_priority": price_group.priority,
            "base_rule_name": price_group.base_rule_name
        })
        return self


class DemandInputTable(TableBase):
    schema = [
        {"name": "sku", "type": "int64"},
        {"name": "price_variant", "type": "double"},
        {"name": "demand_mean", "type": "double"},
        {"name": "demand_smooth", "type": "double"},
    ]

    def add_row(self, sku, price_variant, demand_mean, demand_smooth=None):
        self._rows.append({
            "sku": int(sku),
            "price_variant": float(price_variant),
            "demand_mean": float(demand_mean),
            "demand_smooth": float(demand_smooth) if demand_smooth is not None else None,
        })
        return self


class HistoryTable(TableBase):
    schema = [
        {"name": "group_id", "type": "int64", "sort_order": "ascending"},
        {"name": "date", "type": "string", "sort_order": "ascending"},
        {"name": "offer_price", "type": "double"},
        {"name": "is_exp", "type": "int64"},
        {"name": "item_count", "type": "int64"},
        {"name": "loss", "type": "double"},
        {"name": "gmv_with_coupons", "type": "double"},
        {"name": "sku", "type": "int64"},
        {"name": "purchase_price", "type": "double"},
        {"name": "coupons", "type": "double"}
    ]

    def add_row(self, group_id, date, offer_price, is_exp, item_count, loss, gmv_with_coupons, sku, purchase_price, coupons):
        self._rows.append({
            "group_id": int(group_id),
            "date": date,
            "offer_price": float(offer_price),
            "is_exp": int(is_exp),
            "item_count": int(item_count),
            "loss": float(loss),
            "gmv_with_coupons": float(gmv_with_coupons),
            "sku": int(sku),
            "purchase_price": float(purchase_price),
            "coupons": float(coupons)
        })
        return self


class EstHistoryTable(TableBase):
    schema = [
        {"name": "group_id", "type": "int64", "sort_order": "ascending"},
        {"name": "date", "type": "string", "sort_order": "ascending"},
        {"name": "gmv", "type": "double"},
        {"name": "is_exp", "type": "int64"},
        {"name": "loss", "type": "double"}
    ]

    def add_row(self, group_id, date, is_exp, gmv, loss):
        self._rows.append({
            "group_id": int(group_id),
            "date": date,
            "is_exp": int(is_exp),
            "loss": float(loss),
            "gmv": float(gmv)
        })
        return self


class MarginTable(TableBase):
    schema = [
        {"name": "group_id", "type": "int64"},
        {"name": "margin", "type": "double"}
    ]

    def add_row(self, group_id, margin):
        self._rows.append({
            "group_id": int(group_id),
            "margin": float(margin)
        })
        return self
