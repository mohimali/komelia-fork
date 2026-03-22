ALTER TABLE ImageReaderSettings ADD COLUMN prefetch_spread_count INTEGER NOT NULL DEFAULT 5;
ALTER TABLE ImageReaderSettings ADD COLUMN image_cache_size INTEGER NOT NULL DEFAULT 30;
