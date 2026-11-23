package campus.transfer;

import campus.jdbc.Jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqliteTradeDAO implements TradeDAO {

    private final Connection connection;

    public SqliteTradeDAO(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(Trade trade) throws SQLException {
        String sql = "INSERT INTO trade ("
                + "item_id, initiator_id, owner_id, trade_type, status, message, agreed_price, carbon_reduction"
                + ") VALUES (?, ?, ?, ?, COALESCE(?, 'PENDING'), ?, COALESCE(?, 0), COALESCE(?, 0))";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Jdbc.bind(
                    statement,
                    trade.getItemId(),
                    trade.getInitiatorId(),
                    trade.getOwnerId(),
                    trade.getTradeType(),
                    trade.getStatus(),
                    trade.getMessage(),
                    trade.getAgreedPrice(),
                    trade.getCarbonReduction()
            );
            statement.executeUpdate();
            trade.setId(Jdbc.generatedId(statement));
        }
        Trade stored = findById(trade.getId());
        trade.setStatus(stored.getStatus());
        trade.setAgreedPrice(stored.getAgreedPrice());
        trade.setCarbonReduction(stored.getCarbonReduction());
        trade.setCreatedAt(stored.getCreatedAt());
        trade.setUpdatedAt(stored.getUpdatedAt());
    }

    @Override
    public Trade findById(Long id) throws SQLException {
        if (id == null) {
            return null;
        }
        String sql = "SELECT * FROM trade WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(statement, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    @Override
    public List<Trade> findByItemId(Long itemId) throws SQLException {
        String sql = "SELECT * FROM trade WHERE item_id = ? ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(statement, itemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Trade> trades = new ArrayList<>();
                while (resultSet.next()) {
                    trades.add(map(resultSet));
                }
                return trades;
            }
        }
    }

    @Override
    public int update(Trade trade) throws SQLException {
        String sql = "UPDATE trade SET trade_type = ?, status = ?, message = ?, agreed_price = ?, "
                + "carbon_reduction = ?, completed_at = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(
                    statement,
                    trade.getTradeType(),
                    trade.getStatus(),
                    trade.getMessage(),
                    trade.getAgreedPrice(),
                    trade.getCarbonReduction(),
                    trade.getCompletedAt(),
                    trade.getId()
            );
            return statement.executeUpdate();
        }
    }

    @Override
    public int complete(Long tradeId, double carbonReduction) throws SQLException {
        String sql = "UPDATE trade SET status = 'COMPLETED', carbon_reduction = ?, "
                + "completed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(statement, carbonReduction, tradeId);
            return statement.executeUpdate();
        }
    }

    private Trade map(ResultSet resultSet) throws SQLException {
        Trade trade = new Trade();
        trade.setId(resultSet.getLong("id"));
        trade.setItemId(resultSet.getLong("item_id"));
        trade.setInitiatorId(resultSet.getLong("initiator_id"));
        trade.setOwnerId(resultSet.getLong("owner_id"));
        trade.setTradeType(resultSet.getString("trade_type"));
        trade.setStatus(resultSet.getString("status"));
        trade.setMessage(resultSet.getString("message"));
        trade.setAgreedPrice(resultSet.getDouble("agreed_price"));
        trade.setCarbonReduction(resultSet.getDouble("carbon_reduction"));
        trade.setCreatedAt(resultSet.getString("created_at"));
        trade.setUpdatedAt(resultSet.getString("updated_at"));
        trade.setCompletedAt(resultSet.getString("completed_at"));
        return trade;
    }
}
