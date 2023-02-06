-- http://stackoverflow.com/a/15215436
set ignorecase true;

create table `shard_client_id` (
    `ClientID` int not null primary key,
    `shard` tinyint not null
);

create table `shard_uid` (
    `uid` bigint not null primary key,
    `ClientID` int not null
);

create table `shard_login` (
    `login` varchar(255) not null primary key,
    `uid` bigint not null unique
);

create table `shard_inc_cid` (
    `cid` int not null auto_increment primary key,
    `ClientID` int not null,
    key `ClientID` (`ClientID`)
);

create table `inc_phid` (
    `phid` bigint not null auto_increment primary key
);
