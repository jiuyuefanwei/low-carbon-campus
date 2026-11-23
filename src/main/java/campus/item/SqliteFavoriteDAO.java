package campus.item;

import campus.jdbc.Jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqliteFavoriteDAO implements FavoriteDAO {

    private final Connection connection;

    public SqliteFavoriteDAO(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(Favorite favorite) throws SQLException {
        String sql = "INSERT INTO favorite (user_id, item_id) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Jdbc.bind(statement, favorite.getUserId(), favorite.getItemId());
            statement.executeUpdate();
            favorite.setId(Jdbc.generatedId(statement));
        }
        favorite.setCreatedAt(findByUserAndItem(favorite.getUserId(), favorite.getItemId()).getCreatedAt());
    }

    @Override
    public Favorite findByUserAndItem(Long userId, Long itemId) throws SQLException {
        String sql = "SELECT * FROM favorite WHERE user_id = ? AND item_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(statement, userId, itemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    @Override
    public List<Favorite> findByUserId(Long userId) throws SQLException {
        String sql = "SELECT * FROM favorite WHERE user_id = ? ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(statement, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Favorite> favorites = new ArrayList<>();
                while (resultSet.next()) {
                    favorites.add(map(resultSet));
                }
                return favorites;
            }
        }
    }

    @Override
    public int delete(Long userId, Long itemId) throws SQLException {
        String sql = "DELETE FROM favorite WHERE user_id = ? AND item_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(statement, userId, itemId);
            return statement.executeUpdate();
        }
    }

    private Favorite map(ResultSet resultSet) throws SQLException {
        Favorite favorite = new Favorite();
        favorite.setId(resultSet.getLong("id"));
        favorite.setUserId(resultSet.getLong("user_id"));
        favorite.setItemId(resultSet.getLong("item_id"));
        favorite.setCreatedAt(resultSet.getString("created_at"));
        return favorite;
    }
}
