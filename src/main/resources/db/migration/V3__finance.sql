-- Bei zinazobadilika (kila mabadiliko ni safu mpya, historia inabaki)
create table price_settings (
    id bigserial primary key,
    buy_price_per_dumu bigint not null,
    extra_cost_per_dumu bigint not null default 0,
    sell_price_per_litre bigint not null,
    created_at timestamp not null default now()
);

-- bei za kuanzia (ulizotaja): kununua 86,000 kwa dumu, kuuza 4,800 kwa lita
insert into price_settings (buy_price_per_dumu, extra_cost_per_dumu, sell_price_per_litre)
values (86000, 0, 4800);

-- bei za wakati wa kupeleka oda (ili historia ya mapato isibadilike bei zikibadilika)
alter table orders add column unit_cost bigint;
alter table orders add column unit_price bigint;
alter table orders add column delivered_at timestamp;

-- gharama ya zawadi za lita 1 bure zilizotolewa
create table reward_redemptions (
    id bigserial primary key,
    customer_id bigint not null references customers(id),
    cost bigint not null,
    created_at timestamp not null default now()
);
