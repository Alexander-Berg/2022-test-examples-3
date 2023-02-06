USE hahn;

$partner_ids = (
    SELECT
        id
    FROM
        `home/market/production/mstat/dictionaries/partner_types/latest`
    WHERE
        id IS NOT NULL
);

$state_b2b_partner_path = (
    SELECT
        max(Path)
    FROM
        FOLDER(`home/market/production/mstat/dwh/calculation/state_b2b_partner`)
);

$state_b2b_partner = (
    SELECT
        DISTINCT supplier_id AS id
    FROM
        CONCAT($state_b2b_partner_path)
    WHERE
        supplier_active_assortment_cnt = 0
        AND supplier_id IS NOT NULL
);

$partners_with_programs = (
    SELECT
        partner_id
    FROM
        `home/market/production/mstat/dictionaries/mbi/partner_program_type/latest`
    WHERE
        program IN ('DROPSHIP','FULFILMENT')
        AND partner_id IS NOT NULL
);

$express = (
    SELECT
        DISTINCT partner_id as id
    FROM
        `home/market/production/mbi/dictionaries/partner_service_link/latest`
    WHERE
        is_express = FALSE
        AND partner_id IS NOT NULL
);

$business_id = (
    SELECT
        CAST(business_id AS Int64) as id
    FROM
        `home/market/production/mstat/dictionaries/partner_types/latest`
    WHERE
        business_id IS NOT NULL
);

SELECT
    DISTINCT all_partners.id AS partner_id

FROM $partner_ids AS all_partners

    

    INNER JOIN $partners_with_programs AS partners_with_choosed_programs
        ON all_partners.id = partners_with_choosed_programs.partner_id;
