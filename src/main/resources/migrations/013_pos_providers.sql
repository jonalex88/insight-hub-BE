-- POS Providers

CREATE TABLE IF NOT EXISTS pos_providers (
    id                TEXT PRIMARY KEY,
    name              TEXT NOT NULL,
    short_name        TEXT NOT NULL,
    logo              TEXT,
    description       TEXT,
    primary_industry  TEXT,
    rev_share_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    rev_share_pct     NUMERIC(5, 2),
    sort_order        INTEGER NOT NULL DEFAULT 9999
);

CREATE TABLE IF NOT EXISTS pos_provider_stats (
    pos_provider_id TEXT NOT NULL REFERENCES pos_providers(id) ON DELETE CASCADE,
    date            DATE NOT NULL,
    store_count     INTEGER NOT NULL DEFAULT 0,
    lane_count      INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (pos_provider_id, date)
);

CREATE INDEX IF NOT EXISTS idx_pos_provider_stats_date ON pos_provider_stats(date);

-- Seed providers
INSERT INTO pos_providers (id, name, short_name, primary_industry, sort_order) VALUES
  ('dstore-sigma',     'DStore / Sigma',   'DStore',     'Retail',            1),
  ('roomseeker',       'Roomseeker',        'Roomseeker', 'Hospitality',       2),
  ('big-five-pos',     'Big Five POS',      'Big Five',   'Retail',            3),
  ('unknown-pos',      'Unknown / N/A',     'N/A',        'Unknown',           4),
  ('tradelink',        'Tradelink',         'Tradelink',  'Wholesale',         5),
  ('oracle-opi',       'Oracle OPI',        'Oracle OPI', 'Enterprise',        6),
  ('leroy-merlin-pos', 'Leroy Merlin POS',  'LM POS',     'Retail',            7),
  ('erply',            'Erply',             'Erply',      'Retail',            8),
  ('futura',           'Futura',            'Futura',     'Fashion & Apparel', 9),
  ('syspro',           'Syspro',            'Syspro',     'Manufacturing',    10),
  ('dolphin-pos',      'Dolphin POS',       'Dolphin',    'Retail',           11),
  ('compharm',         'Compharm',          'Compharm',   'Pharmacy',         12),
  ('micros-spi',       'Micros SPI',        'Micros SPI', 'Hospitality',      13),
  ('tj-pos',           'TJ POS',            'TJ POS',     'Retail',           14),
  ('urs-kingsmead',    'URS Kingsmead',     'URSP',       'Retail',           15),
  ('iq-retail',        'IQ Retail',         'IQ Retail',  'Retail',           16),
  ('callpay',          'Callpay',           'Callpay',    'Service',          17),
  ('vexen',            'Vexen',             'Vexen',      'Informal Trade',   18),
  ('calico',           'Calico',            'Calico',     'Fashion & Apparel',19),
  ('unipos',           'Unipos',            'Unipos',     'Retail',           20),
  ('velocity-pos',     'Velocity',          'Velocity',   'Petroleum',        21),
  ('roomseeker-hotel', 'Roomseeker Hotel',  'RS Hotel',   'Hospitality',      22),
  ('micros-oracle',    'Micros (Oracle)',   'Micros',     'Hospitality',      23)
ON CONFLICT (id) DO NOTHING;
