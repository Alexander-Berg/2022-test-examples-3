SET MODE PostgreSQL;

create table "NAVIGATION_TREE_NODE_REDIRECT" (
    tree_code varchar(500) not null,
    from_nid bigint not null,
    to_nid bigint not null,
    regenerate_on_publish boolean default false not null,
    constraint "NAVIGATION_NODE_REDIRECT_U" UNIQUE (tree_code, from_nid)
);
