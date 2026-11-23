package campus.user;

import campus.jdbc.Jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqliteUserDAO implements UserDAO {

    private final Connection connection;

    public SqliteUserDAO(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(User user) throws SQLException {
        String sql = "INSERT INTO user ("
                + "username, password_hash, display_name, phone, email, role, credit_score, status"
                + ") VALUES (?, ?, ?, ?, ?, COALESCE(?, 'STUDENT'), COALESCE(?, 100), COALESCE(?, 'ACTIVE'))";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Jdbc.bind(
                    statement,
                    user.getUsername(),
                    user.getPasswordHash(),
                    user.getDisplayName(),
                    user.getPhone(),
                    user.getEmail(),
                    user.getRole(),
                    user.getCreditScore(),
                    user.getStatus()
            );
            statement.executeUpdate();
            user.setId(Jdbc.generatedId(statement));
        }
        User stored = findById(user.getId());
        user.setRole(stored.getRole());
        user.setCreditScore(stored.getCreditScore());
        user.setStatus(stored.getStatus());
        user.setCreatedAt(stored.getCreatedAt());
        user.setUpdatedAt(stored.getUpdatedAt());
    }

    @Override
    public User findById(Long id) throws SQLException {
        if (id == null) {
            return null;
        }
        String sql = "SELECT * FROM user WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(statement, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    @Override
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM user WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(statement, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    @Override
    public int update(User user) throws SQLException {
        String sql = "UPDATE user SET password_hash = ?, display_name = ?, phone = ?, email = ?, "
                + "role = ?, credit_score = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(
                    statement,
                    user.getPasswordHash(),
                    user.getDisplayName(),
                    user.getPhone(),
                    user.getEmail(),
                    user.getRole(),
                    user.getCreditScore(),
                    user.getStatus(),
                    user.getId()
            );
            return statement.executeUpdate();
        }
    }

    @Override
    public int updateCreditScore(Long userId, int creditScore) throws SQLException {
        String sql = "UPDATE user SET credit_score = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(statement, creditScore, userId);
            return statement.executeUpdate();
        }
    }

    private User map(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getLong("id"));
        user.setUsername(resultSet.getString("username"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setDisplayName(resultSet.getString("display_name"));
        user.setPhone(resultSet.getString("phone"));
        user.setEmail(resultSet.getString("email"));
        user.setRole(resultSet.getString("role"));
        user.setCreditScore(resultSet.getInt("credit_score"));
        user.setStatus(resultSet.getString("status"));
        user.setCreatedAt(resultSet.getString("created_at"));
        user.setUpdatedAt(resultSet.getString("updated_at"));
        return user;
    }
}
