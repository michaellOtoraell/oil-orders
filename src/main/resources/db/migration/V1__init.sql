create table customers (
    id bigserial primary key,
    name varchar(100) not null,
    phone varchar(30),
    location varchar(200),
    token varchar(40) not null unique,
    created_at timestamp not null default now()
);

create table orders (
    id bigserial primary key,
    customer_id bigint not null references customers(id),
    customer_name varchar(100) not null,
    customer_phone varchar(30),
    customer_location varchar(200),
    quantity int not null,
    status varchar(20) not null,
    created_at timestamp not null default now()
);

create index idx_orders_customer_status on orders(customer_id, status);
