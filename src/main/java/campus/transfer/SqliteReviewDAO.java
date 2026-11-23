package campus.transfer;

import campus.jdbc.Jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqliteReviewDAO implements ReviewDAO {

    private final Connection connection;

    public SqliteReviewDAO(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(Review review) throws SQLException {
        String sql = "INSERT INTO review (trade_id, reviewer_id, reviewee_id, rating, content) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Jdbc.bind(
                    statement,
                    review.getTradeId(),
                    review.getReviewerId(),
                    review.getRevieweeId(),
                    review.getRating(),
                    review.getContent()
            );
            statement.executeUpdate();
            review.setId(Jdbc.generatedId(statement));
        }
        review.setCreatedAt(findById(review.getId()).getCreatedAt());
    }

    @Override
    public Review findById(Long id) throws SQLException {
        if (id == null) {
            return null;
        }
        String sql = "SELECT * FROM review WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(statement, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    @Override
    public List<Review> findByTradeId(Long tradeId) throws SQLException {
        String sql = "SELECT * FROM review WHERE trade_id = ? ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(statement, tradeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Review> reviews = new ArrayList<>();
                while (resultSet.next()) {
                    reviews.add(map(resultSet));
                }
                return reviews;
            }
        }
    }

    private Review map(ResultSet resultSet) throws SQLException {
        Review review = new Review();
        review.setId(resultSet.getLong("id"));
        review.setTradeId(resultSet.getLong("trade_id"));
        review.setReviewerId(resultSet.getLong("reviewer_id"));
        review.setRevieweeId(resultSet.getLong("reviewee_id"));
        review.setRating(resultSet.getInt("rating"));
        review.setContent(resultSet.getString("content"));
        review.setCreatedAt(resultSet.getString("created_at"));
        return review;
    }
}
