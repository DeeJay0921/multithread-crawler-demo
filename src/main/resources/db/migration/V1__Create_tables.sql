create table LINKS_TO_BE_PROCESSED
(
	LINK VARCHAR(1000)
);

create table LINKS_ALREADY_PROCESSED
(
	LINK VARCHAR(1000)
);

create table NEWS
(
	ID BIGINT auto_increment,
	TITLE TEXT,
	CONTENT TEXT,
	URL VARCHAR(1000),
	CREATED_AT TIMESTAMP default NOW(),
	UPDATED_AT TIMESTAMP default NOW()
);

create unique index NEWS_ID_uindex
	on NEWS (ID);

alter table NEWS
	add constraint NEWS_pk
		primary key (ID);