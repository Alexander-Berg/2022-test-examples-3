--liquibase formatted sql

--changeset alexlinkevich:1 failOnError:true
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 select count(*) from sys.tables where name = 'GOLD_SKU'
CREATE TABLE wmwhse1.GOLD_SKU (
	SERIALKEY       int NOT NULL IDENTITY(1,1),
	STORERKEY       nvarchar(15) NOT NULL,
    SKU             nvarchar(50) NOT NULL,
	MANUFACTURERSKU nvarchar(150) NOT NULL,
	LIFETIME        int,
    VERSION         bigint NOT NULL,
    ADDDATE         datetime DEFAULT (getutcdate()) NOT NULL,
	EDITDATE        datetime DEFAULT (getutcdate()) NOT NULL,
	ADDWHO          nvarchar(256) DEFAULT (user_name()) NOT NULL,
	EDITWHO         nvarchar(256) DEFAULT (user_name()) NOT NULL,

    CONSTRAINT PK_GOLD_SKU PRIMARY KEY (SKU, STORERKEY),
    CONSTRAINT FK_GOLD_SKU_SKU FOREIGN KEY (SKU, STORERKEY) REFERENCES wmwhse1.SKU (SKU, STORERKEY)
)
--rollback drop table wmwhse1.GOLD_SKU
