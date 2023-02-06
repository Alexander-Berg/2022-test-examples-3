--liquibase formatted sql

--changeset eveber:DELIVERY-33073-add-enable-express-outlets-column
SET LOCK_TIMEOUT = '30s';

ALTER TABLE yt_express_warehouse
    ADD COLUMN IF NOT EXISTS enable_express_outlets BOOLEAN;

--changeset eveber:DELIVERY-32491-express_export_testing_partner runOnChange:true stripComments=true

CREATE OR REPLACE FUNCTION get_enable_express_outlets(w_id BIGINT) RETURNS BOOLEAN
AS
E'
    DECLARE
        count BIGINT;
    BEGIN
        SELECT count(*)
        FROM partner_external_param_value AS pepv
                 JOIN partner_external_param_type AS pept ON pepv.type_id = pept.id
        WHERE pept.key = ''ENABLE_EXPRESS_OUTLETS''
          AND pepv.value = ''1''
          AND pepv.partner_id = w_id
        INTO count;
        RETURN count > 0;
    END;
' LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION perform_data_by_zone_yt_express(z_id BIGINT) RETURNS BOOLEAN
AS
E'
    DECLARE
        partner_ids BIGINT[];
    BEGIN

        SELECT array_agg(l.partner_id::BIGINT)
        FROM logistic_point_radial_location_zone z
                 JOIN logistics_point l ON l.id = z.logistic_point_id
        WHERE z.zone_id = z_id
        INTO partner_ids;

        DELETE
        FROM yt_express_warehouse
        WHERE warehouse_id = ANY (partner_ids);

        INSERT INTO yt_express_warehouse (warehouse_id,
                                          latitude,
                                          longitude,
                                          location_id,
                                          ready_to_ship_time,
                                          business_id,
                                          enable_express_outlets,
                                          radial_zones)
        SELECT p.id                             AS warehouse_id,
               a.latitude                       AS latitude,
               a.longitude                      AS longitude,
               a.location_id                    AS location_id,
               40                               AS ready_to_ship_time,
               p.business_id                    AS business_id,
               get_enable_express_outlets(p.id) AS enable_express_outlets,
               json_agg(
                       json_build_object(
                               ''zone_id'', rz.id,
                               ''radius'', rz.radius,
                               ''delivery_duration'', rz.delivery_duration
                           )
                   )                            AS radial_zones
        FROM radial_location_zone rz
                 JOIN logistic_point_radial_location_zone lprlz ON lprlz.zone_id = rz.id
                 JOIN logistics_point lp ON lprlz.logistic_point_id = lp.id
                 JOIN address a ON lp.address_id = a.id
                 JOIN partner_external_param_value pepv ON lp.partner_id = pepv.partner_id
                 JOIN partner_external_param_type pept ON pepv.type_id = pept.id
                 JOIN partner p ON lp.partner_id = p.id
        WHERE pept.key = ''DROPSHIP_EXPRESS''
          AND pepv.value = ''1''
          AND p.id = ANY (partner_ids)
          AND lp.active
          AND p.status IN (''active'', ''testing'')
        GROUP BY p.id, a.latitude, a.longitude, a.location_id, p.business_id;
        RETURN TRUE;
    END;
' LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION load_data_yt_express(warehouse_id BIGINT) RETURNS BOOLEAN
AS
E'
    BEGIN
        INSERT
        INTO yt_express_warehouse (warehouse_id,
                                   latitude,
                                   longitude,
                                   location_id,
                                   ready_to_ship_time,
                                   business_id,
                                   enable_express_outlets,
                                   radial_zones)
        SELECT p.id                             AS warehouse_id,
               a.latitude                       AS latitude,
               a.longitude                      AS longitude,
               a.location_id                    AS location_id,
               40                               AS ready_to_ship_time,
               p.business_id                    AS business_id,
               get_enable_express_outlets(p.id) AS enable_express_outlets,
               json_agg(
                       json_build_object(
                               ''zone_id'', rz.id,
                               ''radius'', rz.radius,
                               ''delivery_duration'', rz.delivery_duration
                           )
                   )                            AS radial_zones
        FROM radial_location_zone rz
                 JOIN logistic_point_radial_location_zone lprlz ON lprlz.zone_id = rz.id
                 JOIN logistics_point lp ON lprlz.logistic_point_id = lp.id
                 JOIN address a ON lp.address_id = a.id
                 JOIN partner_external_param_value pepv ON lp.partner_id = pepv.partner_id
                 JOIN partner_external_param_type pept ON pepv.type_id = pept.id
                 JOIN partner p ON lp.partner_id = p.id
        WHERE pept.key = ''DROPSHIP_EXPRESS''
          AND pepv.value = ''1''
          AND lp.id = warehouse_id
          AND lp.active
          AND p.status IN (''active'', ''testing'')
        GROUP BY p.id, a.latitude, a.longitude, a.location_id, p.business_id;
        RETURN TRUE;
    END;
' LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION load_data_by_partner_yt_express(p_id BIGINT) RETURNS BOOLEAN
AS
E'
    BEGIN
        INSERT
        INTO yt_express_warehouse (warehouse_id,
                                   latitude,
                                   longitude,
                                   location_id,
                                   ready_to_ship_time,
                                   business_id,
                                   enable_express_outlets,
                                   radial_zones)
        SELECT p.id                             AS warehouse_id,
               a.latitude                       AS latitude,
               a.longitude                      AS longitude,
               a.location_id                    AS location_id,
               40                               AS ready_to_ship_time,
               p.business_id                    AS business_id,
               get_enable_express_outlets(p.id) AS enable_express_outlets,
               json_agg(
                       json_build_object(
                               ''zone_id'', rz.id,
                               ''radius'', rz.radius,
                               ''delivery_duration'', rz.delivery_duration
                           )
                   )                            AS radial_zones
        FROM radial_location_zone rz
                 JOIN logistic_point_radial_location_zone lprlz ON lprlz.zone_id = rz.id
                 JOIN logistics_point lp ON lprlz.logistic_point_id = lp.id
                 JOIN address a ON lp.address_id = a.id
                 JOIN partner_external_param_value pepv ON lp.partner_id = pepv.partner_id
                 JOIN partner_external_param_type pept ON pepv.type_id = pept.id
                 JOIN partner p ON lp.partner_id = p.id
        WHERE pept.key = ''DROPSHIP_EXPRESS''
          AND pepv.value = ''1''
          AND lp.partner_id = p_id
          AND lp.active
          AND lp.type = ''WAREHOUSE''
          AND p.status IN (''active'', ''testing'')
        GROUP BY p.id, a.latitude, a.longitude, a.location_id, p.business_id;

        RETURN TRUE;
    END;
' LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION sync_yt_express_warehouse_func_partner() RETURNS TRIGGER AS
E'
    BEGIN
        IF (new.status != old.status) THEN
            PERFORM delete_data_by_partner_yt_express(old.id);
            IF (new.status IN (''active'', ''testing'')) THEN
                PERFORM load_data_by_partner_yt_express(old.id);
            END IF;
        END IF;
        RETURN NULL;
    END;
' LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION sync_yt_express_warehouse_func_enable_express_outlets_value() RETURNS TRIGGER AS
E'
    DECLARE
        enable_outlets_type_id BIGINT;
    BEGIN
        SELECT id
        FROM partner_external_param_type
        WHERE key = \'ENABLE_EXPRESS_OUTLETS\'
        INTO enable_outlets_type_id;


        IF (tg_op = \'DELETE\') THEN
            IF (old.type_id != enable_outlets_type_id) THEN
                RETURN NULL;
            END IF;
            PERFORM delete_data_by_partner_yt_express(old.partner_id);
            RETURN NULL;
        END IF;

        IF (new.type_id != enable_outlets_type_id) THEN
            RETURN NULL;
        END IF;


        PERFORM delete_data_by_partner_yt_express(new.partner_id);
        PERFORM load_data_by_partner_yt_express(new.partner_id);

        RETURN NULL;
    END;
' LANGUAGE plpgsql;

--changeset eveber:DELIVERY-33073-add-trigger-for-enable-express-outlets-param
SET LOCK_TIMEOUT = '30s';

CREATE TRIGGER sync_yt_express_warehouse_enable_express_outlets_value
    AFTER INSERT OR DELETE OR UPDATE
    ON partner_external_param_value
    FOR EACH ROW
EXECUTE PROCEDURE sync_yt_express_warehouse_func_enable_express_outlets_value();

--changeset eveber:DELIVERY-33589-add-delivery-duration-not-null-constraint
SET LOCK_TIMEOUT = '30s';
ALTER TABLE radial_location_zone
    ALTER COLUMN delivery_duration SET NOT NULL;

--changeset eveber:DELIVERY-33589-add-region_id-not-null-constraint
SET LOCK_TIMEOUT = '30s';
ALTER TABLE radial_location_zone
    ALTER COLUMN region_id SET NOT NULL;


--changeset eveber:DELIVERY-33589-add-radius-not-null-constraint
SET LOCK_TIMEOUT = '30s';
ALTER TABLE radial_location_zone
    ALTER COLUMN radius SET NOT NULL;

--changeset eveber:DELIVERY-33589-add-logistic_point_id-not-null-constraint
SET LOCK_TIMEOUT = '30s';
ALTER TABLE logistic_point_radial_location_zone
    ALTER COLUMN logistic_point_id SET NOT NULL;

--changeset eveber:DELIVERY-33589-add-zone_id-not-null-constraint
SET LOCK_TIMEOUT = '30s';
ALTER TABLE logistic_point_radial_location_zone
    ALTER COLUMN zone_id SET NOT NULL;
