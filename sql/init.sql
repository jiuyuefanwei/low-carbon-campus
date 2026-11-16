
-- =====================================================
-- 低碳校园资源循环流转桌面系统
-- SQLite 数据库初始化脚本
-- 对应文档：docs/04-数据库设计.md
-- =====================================================

-- 开启外键约束
PRAGMA foreign_keys = ON;

-- =====================================================
-- 1. 用户表 user
-- =====================================================
CREATE TABLE IF NOT EXISTS user (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    username        TEXT NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    display_name    TEXT NOT NULL,
    phone           TEXT,
    email           TEXT,
    role            TEXT NOT NULL DEFAULT 'STUDENT'
                    CHECK (role IN ('STUDENT', 'ADMIN')),
    credit_score    INTEGER NOT NULL DEFAULT 100
                    CHECK (credit_score >= 0 AND credit_score <= 100),
    status          TEXT NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at      TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 2. 物品类别表 category
-- =====================================================
CREATE TABLE IF NOT EXISTS category (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT NOT NULL UNIQUE,
    description     TEXT,
    carbon_factor   REAL NOT NULL DEFAULT 0
                    CHECK (carbon_factor >= 0),
    is_active       INTEGER NOT NULL DEFAULT 1
                    CHECK (is_active IN (0, 1)),
    created_at      TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 3. 闲置物品表 item
-- =====================================================
CREATE TABLE IF NOT EXISTS item (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    seller_id       INTEGER NOT NULL,
    category_id     INTEGER NOT NULL,
    name            TEXT NOT NULL,
    description     TEXT,
    expected_price  REAL NOT NULL DEFAULT 0
                    CHECK (expected_price >= 0),
    trade_type      TEXT NOT NULL
                    CHECK (trade_type IN ('SALE', 'EXCHANGE', 'FREE')),
    status          TEXT NOT NULL DEFAULT 'AVAILABLE'
                    CHECK (status IN (
                        'AVAILABLE', 'RESERVED',
                        'COMPLETED', 'OFFLINE'
                    )),
    tags            TEXT,
    image_path      TEXT,
    created_at      TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    taken_down_at   TEXT,

    FOREIGN KEY (seller_id) REFERENCES user(id),
    FOREIGN KEY (category_id) REFERENCES category(id)
);

-- =====================================================
-- 4. 收藏表 favorite
-- =====================================================
CREATE TABLE IF NOT EXISTS favorite (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL,
    item_id         INTEGER NOT NULL,
    created_at      TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (item_id) REFERENCES item(id),

    -- 同一用户不能重复收藏同一物品
    UNIQUE (user_id, item_id)
);

-- =====================================================
-- 5. 流转记录表 trade
-- =====================================================
CREATE TABLE IF NOT EXISTS trade (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id         INTEGER NOT NULL,
    initiator_id    INTEGER NOT NULL,
    owner_id        INTEGER NOT NULL,
    trade_type      TEXT NOT NULL
                    CHECK (trade_type IN ('SALE', 'EXCHANGE', 'FREE')),
    status          TEXT NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN (
                        'PENDING', 'ACCEPTED', 'REJECTED',
                        'COMPLETED', 'CANCELLED'
                    )),
    message         TEXT,
    agreed_price    REAL NOT NULL DEFAULT 0
                    CHECK (agreed_price >= 0),
    carbon_reduction REAL NOT NULL DEFAULT 0
                    CHECK (carbon_reduction >= 0),
    created_at      TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    TEXT,

    FOREIGN KEY (item_id) REFERENCES item(id),
    FOREIGN KEY (initiator_id) REFERENCES user(id),
    FOREIGN KEY (owner_id) REFERENCES user(id),

    -- 发起人不能是物品所有者
    CHECK (initiator_id <> owner_id)
);

-- =====================================================
-- 6. 评价表 review
-- =====================================================
CREATE TABLE IF NOT EXISTS review (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    trade_id        INTEGER NOT NULL,
    reviewer_id     INTEGER NOT NULL,
    reviewee_id     INTEGER NOT NULL,
    rating          INTEGER NOT NULL
                    CHECK (rating BETWEEN 1 AND 5),
    content         TEXT,
    created_at      TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (trade_id) REFERENCES trade(id),
    FOREIGN KEY (reviewer_id) REFERENCES user(id),
    FOREIGN KEY (reviewee_id) REFERENCES user(id),

    -- 同一流转中，同一用户只能评价一次
    UNIQUE (trade_id, reviewer_id),

    -- 评价者不能评价自己
    CHECK (reviewer_id <> reviewee_id)
);

-- =====================================================
-- 7. 索引
-- =====================================================

-- 用户相关查询
CREATE INDEX IF NOT EXISTS idx_user_username
ON user(username);

-- 物品查询：发布者、类别、状态
CREATE INDEX IF NOT EXISTS idx_item_seller
ON item(seller_id);

CREATE INDEX IF NOT EXISTS idx_item_category
ON item(category_id);

CREATE INDEX IF NOT EXISTS idx_item_status
ON item(status);

-- 收藏查询
CREATE INDEX IF NOT EXISTS idx_favorite_user
ON favorite(user_id);

CREATE INDEX IF NOT EXISTS idx_favorite_item
ON favorite(item_id);

-- 流转记录查询
CREATE INDEX IF NOT EXISTS idx_trade_item
ON trade(item_id);

CREATE INDEX IF NOT EXISTS idx_trade_initiator
ON trade(initiator_id);

CREATE INDEX IF NOT EXISTS idx_trade_owner
ON trade(owner_id);

CREATE INDEX IF NOT EXISTS idx_trade_status
ON trade(status);

-- 评价查询
CREATE INDEX IF NOT EXISTS idx_review_trade
ON review(trade_id);

CREATE INDEX IF NOT EXISTS idx_review_reviewee
ON review(reviewee_id);

-- =====================================================
-- 8. 初始化物品类别
-- =====================================================
INSERT OR IGNORE INTO category (name, description, carbon_factor)
VALUES
    ('书籍教材', '教材、课外书籍及学习资料', 0.5),
    ('电子产品', '电脑配件、数码设备等', 2.0),
    ('生活用品', '日常生活用品及宿舍用品', 0.3),
    ('体育用品', '运动器材及相关用品', 0.8),
    ('其他', '其他闲置物品', 0.1);