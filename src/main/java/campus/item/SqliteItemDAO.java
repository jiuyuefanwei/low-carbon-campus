package campus.item;

import campus.jdbc.Jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqliteItemDAO implements ItemDAO {

    private final Connection connection;

    public SqliteItemDAO(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(Item item) throws SQLException {
        String sql = "INSERT INTO item ("
                + "seller_id, category_id, name, description, expected_price, trade_type, status, tags, image_path"
                + ") VALUES (?, ?, ?, ?, COALESCE(?, 0), ?, COALESCE(?, 'AVAILABLE'), ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Jdbc.bind(
                    statement,
                    item.getSellerId(),
                    item.getCategoryId(),
                    item.getName(),
                    item.getDescription(),
                    item.getExpectedPrice(),
                    item.getTradeType(),
                    item.getStatus(),
                    item.getTags(),
                    item.getImagePath()
            );
            statement.executeUpdate();
            item.setId(Jdbc.generatedId(statement));
        }
        Item stored = findById(item.getId());
        item.setExpectedPrice(stored.getExpectedPrice());
        item.setStatus(stored.getStatus());
        item.setCreatedAt(stored.getCreatedAt());
        item.setUpdatedAt(stored.getUpdatedAt());
    }

    @Override
    public Item findById(Long id) throws SQLException {
        if (id == null) {
            return null;
        }
        String sql = "SELECT * FROM item WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(statement, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    @Override
    public List<Item> findByCondition(ItemQuery query) throws SQLException {
        ItemQuery condition = query == null ? new ItemQuery() : query;
        StringBuilder sql = new StringBuilder("SELECT * FROM item WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (condition.getCategoryId() != null) {
            sql.append(" AND category_id = ?");
            params.add(condition.getCategoryId());
        }
        if (condition.getSellerId() != null) {
            sql.append(" AND seller_id = ?");
            params.add(condition.getSellerId());
        }
        if (hasText(condition.getStatus())) {
            sql.append(" AND status = ?");
            params.add(condition.getStatus());
        }
        if (condition.getMinPrice() != null) {
            sql.append(" AND expected_price >= ?");
            params.add(condition.getMinPrice());
        }
        if (condition.getMaxPrice() != null) {
            sql.append(" AND expected_price <= ?");
            params.add(condition.getMaxPrice());
        }
        if (hasText(condition.getKeyword())) {
            sql.append(" AND (name LIKE ? ESCAPE '\\' OR IFNULL(description, '') LIKE ? ESCAPE '\\' "
                    + "OR IFNULL(tags, '') LIKE ? ESCAPE '\\')");
            String pattern = likePattern(condition.getKeyword().trim());
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        sql.append(" ORDER BY id");

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            Jdbc.bind(statement, params.toArray());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Item> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(map(resultSet));
                }
                return items;
            }
        }
    }

    @Override
    public int update(Item item) throws SQLException {
        String sql = "UPDATE item SET category_id = ?, name = ?, description = ?, expected_price = ?, "
                + "trade_type = ?, status = ?, tags = ?, image_path = ?, taken_down_at = ?, "
                + "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(
                    statement,
                    item.getCategoryId(),
                    item.getName(),
                    item.getDescription(),
                    item.getExpectedPrice(),
                    item.getTradeType(),
                    item.getStatus(),
                    item.getTags(),
                    item.getImagePath(),
                    item.getTakenDownAt(),
                    item.getId()
            );
            return statement.executeUpdate();
        }
    }

    @Override
    public int updateStatus(Long itemId, String status) throws SQLException {
        String sql = "UPDATE item SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(statement, status, itemId);
            return statement.executeUpdate();
        }
    }

    @Override
    public int delete(Long id) throws SQLException {
        String sql = "DELETE FROM item WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(statement, id);
            return statement.executeUpdate();
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String likePattern(String keyword) {
        String escaped = keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private Item map(ResultSet resultSet) throws SQLException {
        Item item = new Item();
        item.setId(resultSet.getLong("id"));
        item.setSellerId(resultSet.getLong("seller_id"));
        item.setCategoryId(resultSet.getLong("category_id"));
        item.setName(resultSet.getString("name"));
        item.setDescription(resultSet.getString("description"));
        item.setExpectedPrice(resultSet.getDouble("expected_price"));
        item.setTradeType(resultSet.getString("trade_type"));
        item.setStatus(resultSet.getString("status"));
        item.setTags(resultSet.getString("tags"));
        item.setImagePath(resultSet.getString("image_path"));
        item.setCreatedAt(resultSet.getString("created_at"));
        item.setUpdatedAt(resultSet.getString("updated_at"));
        item.setTakenDownAt(resultSet.getString("taken_down_at"));
        return item;
    }
}
